package com.ai.cs.application.analytics;

import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.ConversationStatus;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import com.ai.cs.domain.conversation.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiEmployeeAnalyticsServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    private AiEmployeeAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AiEmployeeAnalyticsService(conversationRepository, messageRepository);
    }

    @Test
    void getEmployeeStats_shouldReturnCorrectCounts() {
        Long employeeId = 1L;
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);

        Conversation conv1 = new Conversation();
        conv1.setEmployeeId(1L);
        conv1.setStatus(ConversationStatus.CLOSED);
        conv1.setStartTime(LocalDateTime.of(2024, 6, 1, 10, 0));

        Conversation conv2 = new Conversation();
        conv2.setEmployeeId(1L);
        conv2.setStatus(ConversationStatus.AI_ACTIVE);
        conv2.setStartTime(LocalDateTime.of(2024, 6, 2, 10, 0));

        Conversation conv3 = new Conversation();
        conv3.setEmployeeId(1L);
        conv3.setStatus(ConversationStatus.CLOSED);
        conv3.setHumanAgentId(100L);
        conv3.setStartTime(LocalDateTime.of(2024, 6, 3, 10, 0));

        when(conversationRepository.findAll()).thenReturn(List.of(conv1, conv2, conv3));

        AiEmployeeAnalyticsService.EmployeeStats stats = service.getEmployeeStats(employeeId, start, end);

        assertEquals(3, stats.getTotalConversations());
        assertEquals(2, stats.getClosedConversations());
        assertEquals(1, stats.getTransferredConversations());
        assertEquals(1.0 / 3, stats.getTransferRate(), 0.001);
        assertEquals((2.0 - 1) / 3, stats.getResolutionRate(), 0.001);
    }

    @Test
    void getEmployeeStats_shouldReturnZeroWhenNoConversations() {
        when(conversationRepository.findAll()).thenReturn(List.of());

        AiEmployeeAnalyticsService.EmployeeStats stats = service.getEmployeeStats(1L,
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 12, 31, 23, 59));

        assertEquals(0, stats.getTotalConversations());
        assertEquals(0, stats.getClosedConversations());
        assertEquals(0, stats.getTransferredConversations());
        assertEquals(0.0, stats.getTransferRate());
        assertEquals(0.0, stats.getResolutionRate());
    }

    @Test
    void getEmployeeStats_shouldFilterByEmployeeId() {
        Long employeeId = 1L;
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);

        Conversation conv1 = new Conversation();
        conv1.setEmployeeId(1L);
        conv1.setStatus(ConversationStatus.CLOSED);
        conv1.setStartTime(LocalDateTime.of(2024, 6, 1, 10, 0));

        Conversation conv2 = new Conversation();
        conv2.setEmployeeId(2L); // Different employee
        conv2.setStatus(ConversationStatus.CLOSED);
        conv2.setStartTime(LocalDateTime.of(2024, 6, 1, 10, 0));

        when(conversationRepository.findAll()).thenReturn(List.of(conv1, conv2));

        AiEmployeeAnalyticsService.EmployeeStats stats = service.getEmployeeStats(employeeId, start, end);

        assertEquals(1, stats.getTotalConversations());
        assertEquals(1, stats.getClosedConversations());
    }

    @Test
    void getEmployeeStats_shouldFilterByDateRange() {
        Long employeeId = 1L;
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 6, 30, 23, 59);

        Conversation conv1 = new Conversation();
        conv1.setEmployeeId(1L);
        conv1.setStatus(ConversationStatus.CLOSED);
        conv1.setStartTime(LocalDateTime.of(2024, 6, 1, 10, 0)); // In range

        Conversation conv2 = new Conversation();
        conv2.setEmployeeId(1L);
        conv2.setStatus(ConversationStatus.CLOSED);
        conv2.setStartTime(LocalDateTime.of(2024, 7, 1, 10, 0)); // Out of range

        when(conversationRepository.findAll()).thenReturn(List.of(conv1, conv2));

        AiEmployeeAnalyticsService.EmployeeStats stats = service.getEmployeeStats(employeeId, start, end);

        assertEquals(1, stats.getTotalConversations());
    }

    @Test
    void getEmployeeStats_shouldHandleZeroTotalConversationsForRate() {
        when(conversationRepository.findAll()).thenReturn(List.of());

        AiEmployeeAnalyticsService.EmployeeStats stats = service.getEmployeeStats(1L,
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 12, 31, 23, 59));

        assertEquals(0.0, stats.getTransferRate());
        assertEquals(0.0, stats.getResolutionRate());
    }
}
