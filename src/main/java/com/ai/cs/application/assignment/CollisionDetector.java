package com.ai.cs.application.assignment;

import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Collision detector — prevents duplicate assignment by checking
 * if a customer has an active conversation within the last 30 days.
 * If found, auto-assigns to the original agent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollisionDetector {

    private final ConversationRepository conversationRepository;

    /**
     * Detect if customer has an active conversation within 30 days
     * @return original agent ID, or null if no active conversation
     */
    public Long detect(Long customerId) {
        List<Conversation> activeConversations = conversationRepository
                .findActiveByCustomerSince(customerId, LocalDateTime.now().minusDays(30));

        if (activeConversations != null && !activeConversations.isEmpty()) {
            Conversation active = activeConversations.get(0);
            if (active.getOwnerAgentId() != null) {
                log.info("碰单检测命中: customerId={}, agentId={}", customerId, active.getOwnerAgentId());
                return active.getOwnerAgentId();
            }
        }
        return null;
    }
}
