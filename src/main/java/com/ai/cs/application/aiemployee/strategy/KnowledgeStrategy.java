package com.ai.cs.application.aiemployee.strategy;

import com.ai.cs.application.knowledge.SystemPromptBuilder;
import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.infrastructure.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeStrategy implements ReplyStrategyNode {

    private final LlmService llmService;
    private final SystemPromptBuilder promptBuilder;

    @Override
    public StrategyResult process(ConversationContext ctx, ReplyBuilder builder) {
        AiEmployee employee = ctx.getEmployee();

        String systemPrompt = promptBuilder.build(employee, ctx.getKnowledgeChunks(), ctx.getRecentHistory());
        String userMsg = "用户最新消息：" + ctx.getCustomerMessage();

        // Call LLM
        try {
            String reply = llmService.chat(systemPrompt, userMsg);
            if (reply != null && !reply.isEmpty()) {
                builder.append(reply);
                return StrategyResult.REPLIED;
            }
        } catch (Exception e) {
            log.error("LLM 调用失败", e);
        }

        return StrategyResult.CONTINUE;
    }

    @Override
    public int getOrder() {
        return 50;
    }
}
