package com.ai.cs.application.aiemployee.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContactStrategy implements ReplyStrategyNode {

    private static final String DEFAULT_CONTACT =
            "您好，感谢您的咨询！如您需要进一步帮助，请联系我们的客服热线 400-XXX-XXXX。";

    @Override
    public StrategyResult process(ConversationContext ctx, ReplyBuilder builder) {
        if (!builder.hasContent()) {
            log.info("未生成回复内容，使用默认联系信息");
            builder.append(DEFAULT_CONTACT);
        }
        return StrategyResult.REPLIED;
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
