package com.ai.cs.application.analytics;

import com.ai.cs.domain.conversation.ConversationStatus;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import com.ai.cs.domain.conversation.repository.MessageRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiEmployeeAnalyticsService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public EmployeeStats getEmployeeStats(Long employeeId, LocalDateTime start, LocalDateTime end) {
        var conversations = conversationRepository.findAll();

        long totalCount = conversations.stream()
                .filter(c -> c.getEmployeeId().equals(employeeId))
                .filter(c -> !c.getStartTime().isBefore(start) && !c.getStartTime().isAfter(end))
                .count();

        long closedCount = conversations.stream()
                .filter(c -> c.getEmployeeId().equals(employeeId))
                .filter(c -> c.getStatus() == ConversationStatus.CLOSED)
                .filter(c -> !c.getStartTime().isBefore(start) && !c.getStartTime().isAfter(end))
                .count();

        long transferredCount = conversations.stream()
                .filter(c -> c.getEmployeeId().equals(employeeId))
                .filter(c -> c.getHumanAgentId() != null)
                .filter(c -> !c.getStartTime().isBefore(start) && !c.getStartTime().isAfter(end))
                .count();

        EmployeeStats stats = new EmployeeStats();
        stats.setTotalConversations(totalCount);
        stats.setClosedConversations(closedCount);
        stats.setTransferredConversations(transferredCount);
        stats.setTransferRate(totalCount > 0 ? (double) transferredCount / totalCount : 0);
        stats.setResolutionRate(closedCount > 0 && totalCount > 0 ? (double) (closedCount - transferredCount) / totalCount : 0);

        return stats;
    }

    @Data
    public static class EmployeeStats {
        private long totalConversations;
        private long closedConversations;
        private long transferredConversations;
        private double transferRate;
        private double resolutionRate;
    }
}
