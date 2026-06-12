package com.ai.cs.application.assignment.strategy;

import com.ai.cs.domain.assignment.HumanAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeastLoadStrategyTest {

    private LeastLoadStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new LeastLoadStrategy();
    }

    @Test
    void assign_shouldReturnLeastLoadedAgent() {
        HumanAgent agent1 = new HumanAgent();
        agent1.setId(1L);
        agent1.setCurrentLoad(5);
        agent1.setMaxConcurrent(10);

        HumanAgent agent2 = new HumanAgent();
        agent2.setId(2L);
        agent2.setCurrentLoad(2);
        agent2.setMaxConcurrent(10);

        HumanAgent agent3 = new HumanAgent();
        agent3.setId(3L);
        agent3.setCurrentLoad(8);
        agent3.setMaxConcurrent(10);

        HumanAgent result = strategy.assign(List.of(agent1, agent2, agent3), "{}", 1L);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals(2, result.getCurrentLoad());
    }

    @Test
    void assign_shouldExcludeFullyLoadedAgents() {
        HumanAgent agent1 = new HumanAgent();
        agent1.setId(1L);
        agent1.setCurrentLoad(10);
        agent1.setMaxConcurrent(10);

        HumanAgent agent2 = new HumanAgent();
        agent2.setId(2L);
        agent2.setCurrentLoad(3);
        agent2.setMaxConcurrent(10);

        HumanAgent result = strategy.assign(List.of(agent1, agent2), "{}", 1L);

        assertNotNull(result);
        assertEquals(2L, result.getId());
    }

    @Test
    void assign_allFullyLoaded_shouldReturnNull() {
        HumanAgent agent1 = new HumanAgent();
        agent1.setId(1L);
        agent1.setCurrentLoad(10);
        agent1.setMaxConcurrent(10);

        HumanAgent agent2 = new HumanAgent();
        agent2.setId(2L);
        agent2.setCurrentLoad(5);
        agent2.setMaxConcurrent(5);

        HumanAgent result = strategy.assign(List.of(agent1, agent2), "{}", 1L);

        assertNull(result);
    }

    @Test
    void assign_emptyAgents_shouldReturnNull() {
        assertNull(strategy.assign(List.of(), "{}", 1L));
    }

    @Test
    void assign_singleAgent_shouldReturnIfNotFull() {
        HumanAgent agent = new HumanAgent();
        agent.setId(1L);
        agent.setCurrentLoad(3);
        agent.setMaxConcurrent(10);

        HumanAgent result = strategy.assign(List.of(agent), "{}", 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getType_shouldReturnLeastLoad() {
        assertEquals("LEAST_LOAD", strategy.getType());
    }
}
