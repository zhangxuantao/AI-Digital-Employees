package com.ai.cs.application.assignment.strategy;

import com.ai.cs.domain.assignment.HumanAgent;
import com.ai.cs.infrastructure.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundRobinStrategyTest {

    @Mock
    private CacheService cacheService;

    private RoundRobinStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RoundRobinStrategy(cacheService);
    }

    @Test
    void assign_shouldRotateAmongAgents() {
        HumanAgent agent1 = new HumanAgent();
        agent1.setId(1L);
        agent1.setName("张三");
        HumanAgent agent2 = new HumanAgent();
        agent2.setId(2L);
        agent2.setName("李四");
        HumanAgent agent3 = new HumanAgent();
        agent3.setId(3L);
        agent3.setName("王五");

        List<HumanAgent> agents = List.of(agent1, agent2, agent3);

        when(cacheService.increment(anyString())).thenReturn(0L, 1L, 2L, 3L);

        assertEquals(agent1, strategy.assign(agents, "{}", 1L));
        assertEquals(agent2, strategy.assign(agents, "{}", 1L));
        assertEquals(agent3, strategy.assign(agents, "{}", 1L));
        // Should wrap around with modulo
        assertEquals(agent1, strategy.assign(agents, "{}", 1L));
    }

    @Test
    void assign_emptyAgents_shouldReturnNull() {
        assertNull(strategy.assign(List.of(), "{}", 1L));
    }

    @Test
    void assign_singleAgent_shouldAlwaysReturnSame() {
        HumanAgent agent = new HumanAgent();
        agent.setId(1L);

        when(cacheService.increment(anyString())).thenReturn(0L, 1L, 2L);

        assertEquals(agent, strategy.assign(List.of(agent), "{}", 1L));
        assertEquals(agent, strategy.assign(List.of(agent), "{}", 1L));
        assertEquals(agent, strategy.assign(List.of(agent), "{}", 1L));
    }

    @Test
    void getType_shouldReturnRoundRobin() {
        assertEquals("ROUND_ROBIN", strategy.getType());
    }
}
