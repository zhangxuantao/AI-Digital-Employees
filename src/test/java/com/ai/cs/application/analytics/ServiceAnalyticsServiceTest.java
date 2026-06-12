package com.ai.cs.application.analytics;

import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.ConversationStatus;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import com.ai.cs.infrastructure.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceAnalyticsServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private CacheService cacheService;

    private ServiceAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new ServiceAnalyticsService(conversationRepository, cacheService);
    }

    @Test
    void getRealtimeStats_shouldReturnCorrectCounts() {
        Conversation active1 = new Conversation();
        active1.setStatus(ConversationStatus.AI_ACTIVE);
        Conversation active2 = new Conversation();
        active2.setStatus(ConversationStatus.HUMAN);
        Conversation waiting = new Conversation();
        waiting.setStatus(ConversationStatus.WAITING);
        Conversation queued = new Conversation();
        queued.setStatus(ConversationStatus.QUEUED);
        Conversation closed = new Conversation();
        closed.setStatus(ConversationStatus.CLOSED);

        when(conversationRepository.findAll()).thenReturn(List.of(active1, active2, waiting, queued, closed));

        ServiceAnalyticsService.RealtimeStats stats = service.getRealtimeStats();

        assertEquals(2, stats.getActiveConversations());
        assertEquals(1, stats.getWaitingCount());
        assertEquals(1, stats.getQueuedCount());
        assertEquals(4, stats.getTotalOnline());

        verify(cacheService).set(eq("stats:realtime"), any(ServiceAnalyticsService.RealtimeStats.class), eq(Duration.ofSeconds(30)));
    }

    @Test
    void getRealtimeStats_shouldReturnZeroWhenNoConversations() {
        when(conversationRepository.findAll()).thenReturn(List.of());

        ServiceAnalyticsService.RealtimeStats stats = service.getRealtimeStats();

        assertEquals(0, stats.getActiveConversations());
        assertEquals(0, stats.getWaitingCount());
        assertEquals(0, stats.getQueuedCount());
        assertEquals(0, stats.getTotalOnline());
    }

    @Test
    void getRealtimeStats_shouldCacheResult() {
        when(conversationRepository.findAll()).thenReturn(List.of());
        service.getRealtimeStats();

        verify(cacheService).set(eq("stats:realtime"), any(ServiceAnalyticsService.RealtimeStats.class), eq(Duration.ofSeconds(30)));
    }
}
