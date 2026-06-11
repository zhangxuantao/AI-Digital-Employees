package com.ai.cs.application.assignment;

import com.ai.cs.application.assignment.strategy.AssignmentStrategy;
import com.ai.cs.domain.assignment.AssignmentStrategyConfig;
import com.ai.cs.domain.assignment.HumanAgent;
import com.ai.cs.domain.assignment.repository.AssignmentStrategyConfigRepository;
import com.ai.cs.domain.assignment.repository.HumanAgentRepository;
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

    public AssignmentEngine(HumanAgentRepository agentRepository,
                            AssignmentStrategyConfigRepository strategyConfigRepository,
                            List<AssignmentStrategy> strategies) {
        this.agentRepository = agentRepository;
        this.strategyConfigRepository = strategyConfigRepository;
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(AssignmentStrategy::getType, Function.identity()));
    }

    public HumanAgent assign(Long employeeId, Long conversationId, Long customerId) {
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
        return assigned;
    }

    private HumanAgent assignByHistory(Long customerId, List<HumanAgent> availableAgents) {
        return availableAgents.isEmpty() ? null : availableAgents.get(0);
    }
}
