package com.ai.cs.application.aiemployee.strategy;

public interface ReplyStrategyNode {
    StrategyResult process(ConversationContext ctx, ReplyBuilder builder);
    int getOrder();
}
