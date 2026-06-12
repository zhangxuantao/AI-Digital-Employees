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
public class ExcludeStrategy implements ReplyStrategyNode {

    private final AiEmployeeReplyStrategyRepository replyStrategyRepository;
    private final ObjectMapper objectMapper;

    @Override
    public StrategyResult process(ConversationContext ctx, ReplyBuilder builder) {
        List<AiEmployeeReplyStrategy> configs = replyStrategyRepository
                .findByEmployeeIdAndEnabledOrderBySortOrderAsc(ctx.getEmployee().getId(), true);

        AiEmployeeReplyStrategy excludeConfig = configs.stream()
                .filter(c -> "EXCLUDE".equals(c.getStrategyType()))
                .findFirst()
                .orElse(null);

        if (excludeConfig == null || excludeConfig.getConfigJson() == null) {
            return StrategyResult.CONTINUE;
        }

        try {
            JsonNode config = objectMapper.readTree(excludeConfig.getConfigJson());
            JsonNode keywordsNode = config.get("keywords");
            String rejectMessage = config.has("reject_message")
                    ? config.get("reject_message").asText("抱歉，您的消息包含不允许的内容。")
                    : null;

            if (keywordsNode != null && keywordsNode.isArray()) {
                String customerMsg = ctx.getCustomerMessage();
                if (customerMsg == null) {
                    return StrategyResult.CONTINUE;
                }
                for (JsonNode keyword : keywordsNode) {
                    if (customerMsg.contains(keyword.asText())) {
                        log.info("消息包含排除关键词: {}", keyword.asText());
                        if (rejectMessage != null) {
                            builder.append(rejectMessage);
                        }
                        return StrategyResult.INTERRUPT;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析排除策略配置失败", e);
        }

        return StrategyResult.CONTINUE;
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
