package com.ai.cs.application.analytics;

import com.ai.cs.domain.conversation.ConversationStatus;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import com.ai.cs.infrastructure.cache.CacheService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceAnalyticsService {

    private final ConversationRepository conversationRepository;
    private final CacheService cacheService;

    public RealtimeStats getRealtimeStats() {
        var all = conversationRepository.findAll();

        long activeCount = all.stream()
                .filter(c -> c.getStatus() == ConversationStatus.AI_ACTIVE || c.getStatus() == ConversationStatus.HUMAN)
                .count();
        long waitingCount = all.stream()
                .filter(c -> c.getStatus() == ConversationStatus.WAITING).count();
        long queuedCount = all.stream()
                .filter(c -> c.getStatus() == ConversationStatus.QUEUED).count();

        // Cache for 30 seconds
        RealtimeStats stats = new RealtimeStats();
        stats.setActiveConversations(activeCount);
        stats.setWaitingCount(waitingCount);
        stats.setQueuedCount(queuedCount);
        stats.setTotalOnline(activeCount + waitingCount + queuedCount);

        cacheService.set("stats:realtime", stats, Duration.ofSeconds(30));
        return stats;
    }

    @Data
    public static class RealtimeStats {
        private long activeConversations;
        private long waitingCount;
        private long queuedCount;
        private long totalOnline;
    }
}
