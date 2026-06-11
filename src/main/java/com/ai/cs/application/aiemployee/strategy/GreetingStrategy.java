package com.ai.cs.application.aiemployee.strategy;

import com.ai.cs.domain.employee.AiEmployeeReplyStrategy;
import com.ai.cs.domain.employee.repository.AiEmployeeReplyStrategyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GreetingStrategy implements ReplyStrategyNode {

    private final AiEmployeeReplyStrategyRepository replyStrategyRepository;
    private final ObjectMapper objectMapper;

    @Override
    public StrategyResult process(ConversationContext ctx, ReplyBuilder builder) {
        List<AiEmployeeReplyStrategy> configs = replyStrategyRepository
                .findByEmployeeIdAndEnabledOrderBySortOrderAsc(ctx.getEmployee().getId(), true);

        AiEmployeeReplyStrategy greetingConfig = configs.stream()
                .filter(c -> "GREETING".equals(c.getStrategyType()))
                .findFirst()
                .orElse(null);

        if (greetingConfig == null || greetingConfig.getConfigJson() == null) {
            return StrategyResult.CONTINUE;
        }

        try {
            JsonNode config = objectMapper.readTree(greetingConfig.getConfigJson());
            boolean isNew = ctx.getRecentHistory() == null || ctx.getRecentHistory().size() <= 1;

            if (isNew && config.has("first_greeting")) {
                builder.append(config.get("first_greeting").asText());
                log.info("新会话，使用首次问候语");
                return StrategyResult.REPLIED;
            } else if (!isNew && config.has("return_greeting")) {
                builder.append(config.get("return_greeting").asText());
                log.info("老会话，使用回访问候语");
                return StrategyResult.CONTINUE;
            }

            // fallback: use first_greeting if available
            if (config.has("first_greeting")) {
                builder.append(config.get("first_greeting").asText());
                return StrategyResult.REPLIED;
            }
        } catch (Exception e) {
            log.warn("解析问候语策略配置失败", e);
        }

        return StrategyResult.CONTINUE;
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
