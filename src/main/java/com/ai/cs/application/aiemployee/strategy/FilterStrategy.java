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
@Order(70)
@RequiredArgsConstructor
public class FilterStrategy implements ReplyStrategyNode {

    private final AiEmployeeReplyStrategyRepository strategyRepo;
    private final ObjectMapper objectMapper;

    @Override
    public StrategyResult process(ConversationContext ctx, ReplyBuilder builder) {
        var configs = strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(
                ctx.getEmployee().getId(), true);
        Optional<AiEmployeeReplyStrategy> filterConfig = configs.stream()
                .filter(s -> "FILTER".equals(s.getStrategyType()))
                .findFirst();

        if (filterConfig.isEmpty()) return StrategyResult.CONTINUE;

        try {
            JsonNode cfg = objectMapper.readTree(filterConfig.get().getConfigJson());
            JsonNode criteria = cfg.path("criteria");
            if (!criteria.isArray() || criteria.isEmpty()) return StrategyResult.CONTINUE;

            // Check if customer message matches any filter criteria
            String msg = ctx.getCustomerMessage().toLowerCase();
            for (JsonNode criterion : criteria) {
                String keyword = criterion.path("keyword").asText();
                String action = criterion.path("action").asText("ASK");
                String question = criterion.path("question").asText();

                if (!keyword.isEmpty() && msg.contains(keyword.toLowerCase())) {
                    if ("ASK".equals(action) && !question.isEmpty()) {
                        builder.append(question);
                        return StrategyResult.REPLIED;
                    } else if ("EXCLUDE".equals(action)) {
                        builder.append("抱歉，根据您的情况，我们的服务可能暂时无法满足您的需求。");
                        return StrategyResult.INTERRUPT;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("筛选策略配置解析失败", e);
        }

        return StrategyResult.CONTINUE;
    }

    @Override
    public int getOrder() { return 70; }
}
