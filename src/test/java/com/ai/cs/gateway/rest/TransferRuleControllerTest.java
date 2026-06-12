package com.ai.cs.gateway.rest;

import com.ai.cs.domain.assignment.TransferRule;
import com.ai.cs.domain.assignment.repository.TransferRuleRepository;
import com.ai.cs.shared.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TransferRuleController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(authorities = "ai_employee:view")
@ActiveProfiles("test")
class TransferRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransferRuleRepository ruleRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void list_shouldReturnRules() throws Exception {
        TransferRule rule = new TransferRule();
        rule.setId(1L);
        rule.setEmployeeId(100L);
        rule.setTriggerType("KEYWORD");
        rule.setTriggerConfig("{\"keywords\": [\"退款\"]}");
        rule.setPriority(1);
        rule.setEnabled(true);

        when(ruleRepository.findByEmployeeIdAndEnabledOrderByPriorityAsc(100L, true))
                .thenReturn(List.of(rule));

        mockMvc.perform(get("/api/v1/transfer-rules")
                        .param("employeeId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].triggerType").value("KEYWORD"))
                .andExpect(jsonPath("$.data[0].priority").value(1))
                .andExpect(jsonPath("$.data[0].enabled").value(true));
    }

    @Test
    void create_shouldCreateRule() throws Exception {
        TransferRule rule = new TransferRule();
        rule.setId(1L);
        rule.setEmployeeId(100L);
        rule.setTriggerType("KEYWORD");
        rule.setTriggerConfig("{\"keywords\": [\"退款\"]}");
        rule.setActionType("TRANSFER");
        rule.setPriority(5);
        rule.setEnabled(true);

        when(ruleRepository.save(any())).thenReturn(rule);

        mockMvc.perform(post("/api/v1/transfer-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\": 100, \"triggerType\": \"KEYWORD\", " +
                                "\"triggerConfig\": \"{\\\"keywords\\\": [\\\"退款\\\"]}\", " +
                                "\"actionType\": \"TRANSFER\", \"priority\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.triggerType").value("KEYWORD"))
                .andExpect(jsonPath("$.data.priority").value(5))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void create_shouldDefaultEnabledAndPriority() throws Exception {
        TransferRule rule = new TransferRule();
        rule.setId(2L);
        rule.setEmployeeId(100L);
        rule.setTriggerType("MANUAL");
        rule.setEnabled(true);
        rule.setPriority(0);

        when(ruleRepository.save(any())).thenReturn(rule);

        mockMvc.perform(post("/api/v1/transfer-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\": 100, \"triggerType\": \"MANUAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.priority").value(0));
    }

    @Test
    void update_shouldUpdateRule() throws Exception {
        TransferRule existing = new TransferRule();
        existing.setId(1L);
        existing.setEmployeeId(100L);
        existing.setTriggerType("KEYWORD");
        existing.setTriggerConfig("{\"keywords\": [\"退款\"]}");
        existing.setPriority(1);
        existing.setEnabled(true);

        TransferRule updated = new TransferRule();
        updated.setId(1L);
        updated.setEmployeeId(100L);
        updated.setTriggerType("MANUAL");
        updated.setTriggerConfig("{}");
        updated.setPriority(2);
        updated.setEnabled(false);

        when(ruleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(ruleRepository.save(any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/transfer-rules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"triggerType\": \"MANUAL\", \"priority\": 2, \"enabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.triggerType").value("MANUAL"))
                .andExpect(jsonPath("$.data.priority").value(2))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void delete_shouldSoftDeleteRule() throws Exception {
        TransferRule rule = new TransferRule();
        rule.setId(1L);
        rule.setEnabled(true);

        when(ruleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(delete("/api/v1/transfer-rules/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Verify soft delete - setEnabled(false) was called
        org.mockito.Mockito.verify(ruleRepository).save(org.mockito.ArgumentMatchers.argThat(
                (TransferRule r) -> !r.getEnabled()
        ));
    }

    @Test
    @WithMockUser(authorities = "ai_employee:edit")
    void create_withEditAuthority_shouldSucceed() throws Exception {
        TransferRule rule = new TransferRule();
        rule.setId(1L);
        when(ruleRepository.save(any())).thenReturn(rule);

        mockMvc.perform(post("/api/v1/transfer-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\": 100, \"triggerType\": \"KEYWORD\"}"))
                .andExpect(status().isOk());
    }
}
