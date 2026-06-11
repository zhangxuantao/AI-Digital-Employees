package com.ai.cs.application.conversation;

import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.ConversationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ConversationService {
    public Conversation findOrCreateConversation(Long customerId, Long employeeId, String channel) {
        // STUB — will be fully implemented in Task 1.4
        Conversation conv = new Conversation();
        conv.setId(1L);
        conv.setCustomerId(customerId);
        conv.setEmployeeId(employeeId);
        conv.setChannel(channel);
        conv.setStatus(ConversationStatus.AI_ACTIVE);
        conv.setStartTime(LocalDateTime.now());
        log.info("[STUB] findOrCreateConversation: customerId={}, employeeId={}", customerId, employeeId);
        return conv;
    }
}
