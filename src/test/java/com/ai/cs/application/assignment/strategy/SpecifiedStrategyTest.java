package com.ai.cs.application.assignment.strategy;

import com.ai.cs.domain.assignment.HumanAgent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpecifiedStrategyTest {

    private SpecifiedStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SpecifiedStrategy(new ObjectMapper());
    }

    @Test
    void assign_shouldReturnSpecifiedAgent() {
        HumanAgent agent1 = new HumanAgent();
        agent1.setId(1L);
        agent1.setName("张三");
        agent1.setStatus("ONLINE");

        HumanAgent agent2 = new HumanAgent();
        agent2.setId(2L);
        agent2.setName("李四");
        agent2.setStatus("ONLINE");

        List<HumanAgent> agents = List.of(agent1, agent2);
        String config = "{\"targetAgentId\": 2}";

        HumanAgent result = strategy.assign(agents, config, 1L);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("李四", result.getName());
    }

    @Test
    void assign_shouldReturnNullWhenSpecifiedAgentNotFound() {
        HumanAgent agent = new HumanAgent();
        agent.setId(1L);
        agent.setName("张三");

        String config = "{\"targetAgentId\": 999}";

        HumanAgent result = strategy.assign(List.of(agent), config, 1L);

        assertNull(result);
    }

    @Test
    void assign_shouldReturnNullWhenConfigInvalid() {
        HumanAgent agent = new HumanAgent();
        agent.setId(1L);

        HumanAgent result = strategy.assign(List.of(agent), "not-json", 1L);

        assertNull(result);
    }

    @Test
    void assign_emptyAgents_shouldReturnNull() {
        String config = "{\"targetAgentId\": 1}";

        HumanAgent result = strategy.assign(List.of(), config, 1L);

        assertNull(result);
    }

    @Test
    void getType_shouldReturnSpecified() {
        assertEquals("SPECIFIED", strategy.getType());
    }
}
