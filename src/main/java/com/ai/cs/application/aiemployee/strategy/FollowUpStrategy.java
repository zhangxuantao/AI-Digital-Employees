package com.ai.cs.application.aiemployee.strategy;

import com.ai.cs.domain.employee.AiEmployeeReplyStrategy;
import com.ai.cs.domain.employee.repository.AiEmployeeReplyStrategyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@Order(80)
@RequiredArgsConstructor
public class FollowUpStrategy implements ReplyStrategyNode {

    private final AiEmployeeReplyStrategyRepository strategyRepo;
    private final ObjectMapper objectMapper;

    @Override
    public StrategyResult process(ConversationContext ctx, ReplyBuilder builder) {
        var configs = strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(
                ctx.getEmployee().getId(), true);
        Optional<AiEmployeeReplyStrategy> followUpConfig = configs.stream()
                .filter(s -> "FOLLOWUP".equals(s.getStrategyType()))
                .findFirst();

        if (followUpConfig.isEmpty()) return StrategyResult.CONTINUE;

        // FollowUp only triggers on empty responses (already handled by scheduled task in Phase 2)
        // For Phase 1, just pass through
        if (!builder.hasContent()) {
            try {
                JsonNode cfg = objectMapper.readTree(followUpConfig.get().getConfigJson());
                JsonNode intervals = cfg.path("intervals");
                if (intervals.isArray() && intervals.size() > 0) {
                    String fixed = intervals.get(0).path("fixed").asText(null);
                    if (fixed != null) builder.append(fixed);
                }
            } catch (Exception e) {
                log.warn("沉默追问配置解析失败", e);
            }
        }

        return StrategyResult.CONTINUE;
    }

    @Override
    public int getOrder() { return 80; }
}
