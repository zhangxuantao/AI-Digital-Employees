package com.ai.cs.gateway.rest;

import com.ai.cs.domain.assignment.HumanAgent;
import com.ai.cs.domain.assignment.repository.HumanAgentRepository;
import com.ai.cs.shared.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HumanAgentController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(authorities = "agent:view")
@ActiveProfiles("test")
class HumanAgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HumanAgentRepository agentRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void list_shouldReturnAllAgents() throws Exception {
        HumanAgent agent = new HumanAgent();
        agent.setId(1L);
        agent.setName("张三");
        agent.setPhone("13800138000");
        agent.setStatus("ONLINE");
        agent.setCurrentLoad(3);
        agent.setMaxConcurrent(5);

        when(agentRepository.findAll()).thenReturn(List.of(agent));

        mockMvc.perform(get("/api/v1/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("张三"))
                .andExpect(jsonPath("$.data[0].phone").value("13800138000"))
                .andExpect(jsonPath("$.data[0].status").value("ONLINE"))
                .andExpect(jsonPath("$.data[0].currentLoad").value(3))
                .andExpect(jsonPath("$.data[0].maxConcurrent").value(5));
    }

    @Test
    void get_shouldReturnAgentById() throws Exception {
        HumanAgent agent = new HumanAgent();
        agent.setId(1L);
        agent.setName("张三");
        agent.setStatus("ONLINE");

        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));

        mockMvc.perform(get("/api/v1/agents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("张三"))
                .andExpect(jsonPath("$.data.status").value("ONLINE"));
    }

    @Test
    @WithMockUser(authorities = {"agent:view", "agent:edit"})
    void create_shouldCreateAgentWithDefaults() throws Exception {
        HumanAgent saved = new HumanAgent();
        saved.setId(1L);
        saved.setName("李四");
        saved.setPhone("13900139000");
        saved.setPasswordHash("encoded_password");
        saved.setStatus("OFFLINE");
        saved.setCurrentLoad(0);
        saved.setMaxConcurrent(5);

        when(passwordEncoder.encode("13900139000")).thenReturn("encoded_password");
        when(agentRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"李四\", \"phone\": \"13900139000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("李四"))
                .andExpect(jsonPath("$.data.status").value("OFFLINE"))
                .andExpect(jsonPath("$.data.currentLoad").value(0))
                .andExpect(jsonPath("$.data.maxConcurrent").value(5));
    }

    @Test
    @WithMockUser(authorities = {"agent:view", "agent:edit"})
    void create_shouldUseProvidedMaxConcurrent() throws Exception {
        HumanAgent saved = new HumanAgent();
        saved.setId(2L);
        saved.setName("王五");
        saved.setPhone("13700137000");
        saved.setPasswordHash("encoded");
        saved.setStatus("OFFLINE");
        saved.setCurrentLoad(0);
        saved.setMaxConcurrent(10);

        when(passwordEncoder.encode("13700137000")).thenReturn("encoded");
        when(agentRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"王五\", \"phone\": \"13700137000\", \"maxConcurrent\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.maxConcurrent").value(10));
    }

    @Test
    @WithMockUser(authorities = {"agent:view", "agent:edit"})
    void delete_shouldDeleteAgent() throws Exception {
        mockMvc.perform(delete("/api/v1/agents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
