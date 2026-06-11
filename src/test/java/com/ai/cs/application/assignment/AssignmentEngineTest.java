package com.ai.cs.application.assignment;

import com.ai.cs.application.assignment.strategy.AssignmentStrategy;
import com.ai.cs.application.assignment.strategy.HistoryStrategy;
import com.ai.cs.application.assignment.strategy.RoundRobinStrategy;
import com.ai.cs.application.assignment.strategy.SpecifiedStrategy;
import com.ai.cs.domain.assignment.AssignmentStrategyConfig;
import com.ai.cs.domain.assignment.HumanAgent;
import com.ai.cs.domain.assignment.repository.AssignmentStrategyConfigRepository;
import com.ai.cs.domain.assignment.repository.HumanAgentRepository;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import com.ai.cs.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentEngineTest {

    @Mock
    private HumanAgentRepository agentRepository;
    @Mock
    private AssignmentStrategyConfigRepository strategyConfigRepository;
    @Mock
    private ConversationRepository conversationRepository;

    private AssignmentEngine engine;
    private RoundRobinStrategy roundRobin;
    private SpecifiedStrategy specified;
    private HistoryStrategy history;

    @BeforeEach
    void setUp() {
        roundRobin = new RoundRobinStrategy(null) {
            @Override
            public HumanAgent assign(List<HumanAgent> availableAgents, String configJson, Long employeeId) {
                return availableAgents.isEmpty() ? null : availableAgents.get(0);
            }
        };
        specified = new SpecifiedStrategy(new ObjectMapper());
        history = new HistoryStrategy(conversationRepository);
        List<AssignmentStrategy> strategies = List.of(specified, roundRobin, history);
        engine = new AssignmentEngine(agentRepository, strategyConfigRepository, strategies);
    }

    @Test
    void assign_shouldUseConfiguredStrategy() {
        AssignmentStrategyConfig config = new AssignmentStrategyConfig();
        config.setStrategyType("SPECIFIED");
        config.setConfigJson("{\"targetAgentId\": 2}");
        config.setIsActive(true);

        HumanAgent agent1 = new HumanAgent();
        agent1.setId(1L);
        agent1.setName("张三");
        agent1.setStatus("ONLINE");
        agent1.setCurrentLoad(3);
        agent1.setMaxConcurrent(10);

        HumanAgent agent2 = new HumanAgent();
        agent2.setId(2L);
        agent2.setName("李四");
        agent2.setStatus("ONLINE");
        agent2.setCurrentLoad(1);
        agent2.setMaxConcurrent(10);

        when(strategyConfigRepository.findByEmployeeIdAndIsActive(100L, true))
                .thenReturn(List.of(config));
        when(agentRepository.findByStatusInAndCurrentLoadLessThan(anyList(), anyInt()))
                .thenReturn(List.of(agent1, agent2));

        HumanAgent result = engine.assign(100L, 1L, 10L);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("李四", result.getName());
        // Load should be incremented
        assertEquals(2, result.getCurrentLoad());
    }

    @Test
    void assign_noConfig_shouldThrowException() {
        when(strategyConfigRepository.findByEmployeeIdAndIsActive(100L, true))
                .thenReturn(List.of());

        assertThrows(BusinessException.class, () -> engine.assign(100L, 1L, 10L));
    }

    @Test
    void assign_noAvailableAgents_shouldThrowException() {
        AssignmentStrategyConfig config = new AssignmentStrategyConfig();
        config.setStrategyType("ROUND_ROBIN");
        config.setConfigJson("{}");
        config.setIsActive(true);

        when(strategyConfigRepository.findByEmployeeIdAndIsActive(100L, true))
                .thenReturn(List.of(config));
        when(agentRepository.findByStatusInAndCurrentLoadLessThan(anyList(), anyInt()))
                .thenReturn(List.of());

        assertThrows(BusinessException.class, () -> engine.assign(100L, 1L, 10L));
    }

    @Test
    void assign_unknownStrategy_shouldThrowException() {
        AssignmentStrategyConfig config = new AssignmentStrategyConfig();
        config.setStrategyType("UNKNOWN_STRATEGY");
        config.setConfigJson("{}");
        config.setIsActive(true);

        when(strategyConfigRepository.findByEmployeeIdAndIsActive(100L, true))
                .thenReturn(List.of(config));
        when(agentRepository.findByStatusInAndCurrentLoadLessThan(anyList(), anyInt()))
                .thenReturn(List.of(new HumanAgent()));

        assertThrows(BusinessException.class, () -> engine.assign(100L, 1L, 10L));
    }

    @Test
    void assign_specifiedStrategyAgentNotFound_shouldFallbackToRoundRobin() {
        AssignmentStrategyConfig config = new AssignmentStrategyConfig();
        config.setStrategyType("SPECIFIED");
        config.setConfigJson("{\"targetAgentId\": 999}");
        config.setIsActive(true);

        HumanAgent agent = new HumanAgent();
        agent.setId(1L);
        agent.setName("张三");
        agent.setStatus("ONLINE");
        agent.setCurrentLoad(3);
        agent.setMaxConcurrent(10);

        when(strategyConfigRepository.findByEmployeeIdAndIsActive(100L, true))
                .thenReturn(List.of(config));
        when(agentRepository.findByStatusInAndCurrentLoadLessThan(anyList(), anyInt()))
                .thenReturn(List.of(agent));

        HumanAgent result = engine.assign(100L, 1L, 10L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void assign_historyStrategy_shouldFallbackToFirstAvailable() {
        AssignmentStrategyConfig config = new AssignmentStrategyConfig();
        config.setStrategyType("HISTORY");
        config.setConfigJson("{}");
        config.setIsActive(true);

        HumanAgent agent = new HumanAgent();
        agent.setId(1L);
        agent.setName("张三");
        agent.setStatus("ONLINE");
        agent.setCurrentLoad(3);
        agent.setMaxConcurrent(10);

        when(strategyConfigRepository.findByEmployeeIdAndIsActive(100L, true))
                .thenReturn(List.of(config));
        when(agentRepository.findByStatusInAndCurrentLoadLessThan(anyList(), anyInt()))
                .thenReturn(List.of(agent));

        HumanAgent result = engine.assign(100L, 1L, 10L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
}
