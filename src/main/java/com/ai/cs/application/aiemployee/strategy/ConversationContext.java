package com.ai.cs.application.aiemployee.strategy;

import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.customer.CustomerProfile;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConversationContext {
    private Long conversationId;
    private Conversation conversation;
    private AiEmployee employee;
    private CustomerProfile customer;
    private String customerMessage;
    private List<String> recentHistory;
    private List<String> knowledgeChunks;
    private String generatedReply;
}
