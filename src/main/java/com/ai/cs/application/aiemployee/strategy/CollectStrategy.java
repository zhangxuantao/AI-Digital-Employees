package com.ai.cs.application.aiemployee.strategy;

import com.ai.cs.domain.customer.CustomerProfile;
import com.ai.cs.domain.customer.repository.CustomerProfileRepository;
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
@Order(60)
@RequiredArgsConstructor
public class CollectStrategy implements ReplyStrategyNode {

    private final AiEmployeeReplyStrategyRepository strategyRepo;
    private final CustomerProfileRepository customerRepo;
    private final ObjectMapper objectMapper;

    @Override
    public StrategyResult process(ConversationContext ctx, ReplyBuilder builder) {
        var configs = strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(
                ctx.getEmployee().getId(), true);
        Optional<AiEmployeeReplyStrategy> collectConfig = configs.stream()
                .filter(s -> "COLLECT".equals(s.getStrategyType()))
                .findFirst();

        if (collectConfig.isEmpty()) return StrategyResult.CONTINUE;

        try {
            JsonNode cfg = objectMapper.readTree(collectConfig.get().getConfigJson());
            JsonNode fields = cfg.path("fields");
            if (!fields.isArray() || fields.isEmpty()) return StrategyResult.CONTINUE;

            CustomerProfile customer = ctx.getCustomer();
            if (customer == null) return StrategyResult.CONTINUE;

            // Check which fields are still missing
            for (JsonNode field : fields) {
                String fieldName = field.path("name").asText();
                String question = field.path("question").asText();

                // Check if already collected
                String extraFields = customer.getExtraFields();
                if (extraFields != null && extraFields.contains("\"" + fieldName + "\"")) {
                    continue; // already collected
                }

                builder.append(question);
                return StrategyResult.REPLIED; // Ask one question at a time
            }
        } catch (Exception e) {
            log.warn("收集策略配置解析失败", e);
        }

        return StrategyResult.CONTINUE;
    }

    @Override
    public int getOrder() { return 60; }
}
