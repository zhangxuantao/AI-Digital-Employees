package com.ai.cs.application.aiemployee.strategy;

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

    @Override
    public StrategyResult process(ConversationContext ctx, ReplyBuilder builder) {
        AiEmployee employee = ctx.getEmployee();

        // Build system prompt from employee config
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是").append(employee.getName()).append("，一个专业的客服人员。\n");
        systemPrompt.append("公司介绍：").append(employee.getCompanyIntro()).append("\n");
        systemPrompt.append("产品介绍：").append(employee.getProductIntro()).append("\n");

        if (employee.getServiceScope() != null && !employee.getServiceScope().isEmpty()) {
            systemPrompt.append("服务范围：").append(employee.getServiceScope()).append("\n");
        }
        systemPrompt.append("回复风格：").append(employee.getStyle()).append("\n");
        systemPrompt.append("回复长度：").append(employee.getReplyLength()).append("\n");

        // Add knowledge chunks if present
        if (ctx.getKnowledgeChunks() != null && !ctx.getKnowledgeChunks().isEmpty()) {
            systemPrompt.append("\n参考知识：\n");
            for (String chunk : ctx.getKnowledgeChunks()) {
                systemPrompt.append("- ").append(chunk).append("\n");
            }
        }

        // Build user message with conversation history
        StringBuilder userMsg = new StringBuilder();
        if (ctx.getRecentHistory() != null && !ctx.getRecentHistory().isEmpty()) {
            userMsg.append("对话历史：\n");
            for (String history : ctx.getRecentHistory()) {
                userMsg.append(history).append("\n");
            }
            userMsg.append("\n");
        }
        userMsg.append("用户最新消息：").append(ctx.getCustomerMessage());

        // Call LLM
        try {
            String reply = llmService.chat(systemPrompt.toString(), userMsg.toString());
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
