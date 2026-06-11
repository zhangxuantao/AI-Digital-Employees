package com.ai.cs.application.assignment.strategy;

import com.ai.cs.domain.assignment.HumanAgent;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HistoryStrategyTest {

    @Mock
    private ConversationRepository conversationRepository;

    private HistoryStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new HistoryStrategy(conversationRepository);
    }

    @Test
    void assign_shouldReturnNullDelegatingToEngine() {
        HumanAgent agent = new HumanAgent();
        agent.setId(1L);

        HumanAgent result = strategy.assign(List.of(agent), "{}", 1L);

        assertNull(result, "HistoryStrategy should return null and delegate to AssignmentEngine");
    }

    @Test
    void getType_shouldReturnHistory() {
        assertEquals("HISTORY", strategy.getType());
    }
}
