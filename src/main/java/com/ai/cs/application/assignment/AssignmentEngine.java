package com.ai.cs.application.assignment;

import com.ai.cs.application.assignment.strategy.AssignmentStrategy;
import com.ai.cs.domain.assignment.AssignmentStrategyConfig;
import com.ai.cs.domain.assignment.HumanAgent;
import com.ai.cs.domain.assignment.repository.AssignmentStrategyConfigRepository;
import com.ai.cs.domain.assignment.repository.HumanAgentRepository;
import com.ai.cs.infrastructure.mq.AssignmentNotificationProducer;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AssignmentEngine {

    private final HumanAgentRepository agentRepository;
    private final AssignmentStrategyConfigRepository strategyConfigRepository;
    private final Map<String, AssignmentStrategy> strategyMap;
    private final CollisionDetector collisionDetector;
    private final AssignmentNotificationProducer notificationProducer;

    public AssignmentEngine(HumanAgentRepository agentRepository,
                            AssignmentStrategyConfigRepository strategyConfigRepository,
                            List<AssignmentStrategy> strategies,
                            CollisionDetector collisionDetector,
                            AssignmentNotificationProducer notificationProducer) {
        this.agentRepository = agentRepository;
        this.strategyConfigRepository = strategyConfigRepository;
        this.collisionDetector = collisionDetector;
        this.notificationProducer = notificationProducer;
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(AssignmentStrategy::getType, Function.identity()));
    }

    public HumanAgent assign(Long employeeId, Long conversationId, Long customerId) {
        // 0. Collision detection — check if customer already has active conversation
        Long existingAgentId = collisionDetector.detect(customerId);
        if (existingAgentId != null) {
            HumanAgent existing = agentRepository.findById(existingAgentId).orElse(null);
            if (existing != null && existing.getCurrentLoad() < existing.getMaxConcurrent()) {
                log.info("碰单回流: customerId={}, agentId={}", customerId, existingAgentId);
                existing.setCurrentLoad(existing.getCurrentLoad() + 1);
                agentRepository.save(existing);
                notificationProducer.send(existingAgentId, conversationId);
                return existing;
            }
        }

        AssignmentStrategyConfig config = strategyConfigRepository
                .findByEmployeeIdAndIsActive(employeeId, true)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.STRATEGY_NOT_FOUND, "未配置生效的分配策略"));

        List<HumanAgent> availableAgents = agentRepository
                .findByStatusInAndCurrentLoadLessThan(
                        List.of("ONLINE", "BUSY"), Integer.MAX_VALUE);

        if (availableAgents.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_AVAILABLE_AGENT);
        }

        AssignmentStrategy strategy = strategyMap.get(config.getStrategyType());
        if (strategy == null) {
            throw new BusinessException(ErrorCode.STRATEGY_NOT_FOUND, "未知分配策略: " + config.getStrategyType());
        }

        HumanAgent assigned = strategy.assign(availableAgents, config.getConfigJson(), employeeId);

        if (assigned == null && "HISTORY".equals(config.getStrategyType())) {
            assigned = assignByHistory(customerId, availableAgents);
        }

        if (assigned == null) {
            AssignmentStrategy rr = strategyMap.get("ROUND_ROBIN");
            if (rr != null) assigned = rr.assign(availableAgents, "{}", employeeId);
        }

        if (assigned == null) {
            throw new BusinessException(ErrorCode.NO_AVAILABLE_AGENT);
        }

        assigned.setCurrentLoad(assigned.getCurrentLoad() + 1);
        agentRepository.save(assigned);

        log.info("分配结果: agentId={}, strategy={}", assigned.getId(), config.getStrategyType());
        // Send assignment notification
        notificationProducer.send(assigned.getId(), conversationId);
        return assigned;
    }

    private HumanAgent assignByHistory(Long customerId, List<HumanAgent> availableAgents) {
        return availableAgents.isEmpty() ? null : availableAgents.get(0);
    }
}
