package com.ai.cs.application.knowledge;

import com.ai.cs.domain.employee.AiEmployee;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Assembles the System Prompt for AI customer service agents
 */
@Component
public class SystemPromptBuilder {

    /**
     * Build a complete System Prompt from AI employee config, knowledge chunks, and history
     *
     * @param employee       AI employee config
     * @param knowledgeChunks retrieved knowledge base chunks
     * @param history         recent conversation history
     * @return complete System Prompt string
     */
    public String build(AiEmployee employee, List<String> knowledgeChunks, List<String> history) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是").append(employee.getName()).append("，一个专业的客服人员。\n");
        sb.append("公司介绍：").append(employee.getCompanyIntro()).append("\n");
        sb.append("产品介绍：").append(employee.getProductIntro()).append("\n");

        if (employee.getServiceScope() != null && !employee.getServiceScope().isEmpty()) {
            sb.append("服务范围：").append(employee.getServiceScope()).append("\n");
        }

        sb.append("回复风格：").append(getStyleName(employee.getStyle())).append("\n");
        sb.append("回复长度：").append(getLengthName(employee.getReplyLength())).append("\n");

        if (knowledgeChunks != null && !knowledgeChunks.isEmpty()) {
            sb.append("\n参考知识：\n");
            for (String chunk : knowledgeChunks) {
                sb.append("- ").append(chunk).append("\n");
            }
        }

        if (history != null && !history.isEmpty()) {
            sb.append("\n对话历史：\n");
            for (String h : history) {
                sb.append(h).append("\n");
            }
        }

        return sb.toString();
    }

    private String getStyleName(String style) {
        return switch (style != null ? style : "") {
            case "WARM" -> "亲切热情";
            case "ENTHUSIASTIC" -> "热情洋溢";
            case "RELIABLE" -> "稳重靠谱";
            default -> "专业严谨";
        };
    }

    private String getLengthName(String length) {
        return switch (length != null ? length : "") {
            case "SHORT" -> "简短（约20字以内）";
            case "DETAIL" -> "详细（约60-100字）";
            default -> "适中（约20-30字）";
        };
    }
}
