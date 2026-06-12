package com.ai.cs.application.assignment.strategy;

import com.ai.cs.domain.assignment.HumanAgent;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HistoryStrategy implements AssignmentStrategy {
    private final ConversationRepository conversationRepository;

    @Override public String getType() { return "HISTORY"; }

    @Override
    public HumanAgent assign(List<HumanAgent> availableAgents, String configJson, Long employeeId) {
        // This needs customerId from context; for phase 1, delegate to AssignmentEngine
        return null;
    }
}
