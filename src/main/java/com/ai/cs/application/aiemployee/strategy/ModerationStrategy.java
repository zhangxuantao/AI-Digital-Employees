package com.ai.cs.application.aiemployee.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class ModerationStrategy implements ReplyStrategyNode {

    private static final List<String> TRANSFER_KEYWORDS = Arrays.asList(
            "投诉", "退款", "举报", "赔偿", "差评", "人工客服", "转人工", "找人工", "曝光"
    );

    @Override
    public StrategyResult process(ConversationContext ctx, ReplyBuilder builder) {
        String message = ctx.getCustomerMessage();
        if (message == null) {
            return StrategyResult.CONTINUE;
        }

        boolean matched = TRANSFER_KEYWORDS.stream().anyMatch(message::contains);
        if (matched) {
            log.info("消息触发转人工策略: {}", message);
            builder.append("已为您转接人工客服，请稍候...");
            return StrategyResult.INTERRUPT;
        }

        return StrategyResult.CONTINUE;
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
