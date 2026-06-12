package com.ai.cs.gateway.rest;

import com.ai.cs.application.aiemployee.AiEmployeeService;
import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.domain.employee.AiEmployeeReplyStrategy;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import com.ai.cs.shared.security.JwtTokenProvider;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AiEmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(authorities = "ai_employee:view")
@ActiveProfiles("test")
class AiEmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiEmployeeService aiEmployeeService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private AiEmployee createTestEmployee(Long id, String name) {
        AiEmployee e = new AiEmployee();
        e.setId(id);
        e.setName(name);
        e.setStatus("ENABLED");
        e.setStyle("PROFESSIONAL");
        e.setReplyLength("MEDIUM");
        e.setAggregateInterval(3);
        e.setDelayInterval(2);
        e.setCompanyIntro("Test intro");
        e.setProductIntro("Test product");
        return e;
    }

    private AiEmployeeReplyStrategy createTestStrategy(Long id, Long employeeId, String strategyType) {
        AiEmployeeReplyStrategy s = new AiEmployeeReplyStrategy();
        s.setId(id);
        s.setEmployeeId(employeeId);
        s.setStrategyType(strategyType);
        s.setEnabled(true);
        s.setSortOrder(0);
        return s;
    }

    @Test
    void list_shouldReturnAllEmployees() throws Exception {
        List<AiEmployee> employees = List.of(
                createTestEmployee(1L, "AI客服1"),
                createTestEmployee(2L, "AI客服2")
        );
        when(aiEmployeeService.listAll()).thenReturn(employees);

        mockMvc.perform(get("/api/v1/ai-employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("AI客服1"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("AI客服2"));
    }

    @Test
    void getById_shouldReturnEmployee() throws Exception {
        AiEmployee e = createTestEmployee(1L, "AI客服1");
        when(aiEmployeeService.getById(1L)).thenReturn(e);

        mockMvc.perform(get("/api/v1/ai-employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("AI客服1"));
    }

    @Test
    void getById_shouldReturnErrorForNonExistent() throws Exception {
        when(aiEmployeeService.getById(999L))
                .thenThrow(new BusinessException(ErrorCode.AI_EMPLOYEE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/ai-employees/999"))
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("AI员工不存在"));
    }

    @Test
    @WithMockUser(authorities = {"ai_employee:view", "ai_employee:edit"})
    void create_shouldReturnCreatedEmployeeWithDefaults() throws Exception {
        AiEmployee saved = createTestEmployee(1L, "新AI客服");
        when(aiEmployeeService.create(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/ai-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新AI客服\",\"companyIntro\":\"公司简介\",\"productIntro\":\"产品介绍\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("新AI客服"));
    }

    @Test
    @WithMockUser(authorities = {"ai_employee:view", "ai_employee:edit"})
    void update_shouldPartiallyUpdateEmployee() throws Exception {
        AiEmployee updated = createTestEmployee(1L, "更新后的AI客服");
        updated.setStyle("FRIENDLY");
        when(aiEmployeeService.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/ai-employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"更新后的AI客服\",\"style\":\"FRIENDLY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("更新后的AI客服"))
                .andExpect(jsonPath("$.data.style").value("FRIENDLY"));
    }

    @Test
    @WithMockUser(authorities = {"ai_employee:view", "ai_employee:edit"})
    void delete_shouldSoftDeleteEmployee() throws Exception {
        doNothing().when(aiEmployeeService).delete(1L);

        mockMvc.perform(delete("/api/v1/ai-employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void getStrategies_shouldReturnStrategyList() throws Exception {
        List<AiEmployeeReplyStrategy> strategies = List.of(
                createTestStrategy(1L, 1L, "GREETING"),
                createTestStrategy(2L, 1L, "KNOWLEDGE")
        );
        when(aiEmployeeService.getStrategies(1L)).thenReturn(strategies);

        mockMvc.perform(get("/api/v1/ai-employees/1/strategies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].strategyType").value("GREETING"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].strategyType").value("KNOWLEDGE"));
    }

    @Test
    @WithMockUser(authorities = {"ai_employee:view", "ai_employee:edit"})
    void saveStrategy_shouldCreateStrategy() throws Exception {
        AiEmployeeReplyStrategy saved = createTestStrategy(1L, 1L, "GREETING");
        when(aiEmployeeService.saveStrategy(eq(1L), any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/ai-employees/1/strategies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"strategyType\":\"GREETING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.strategyType").value("GREETING"));
    }

    @Test
    @WithMockUser(authorities = {"ai_employee:view", "ai_employee:edit"})
    void deleteStrategy_shouldSoftDeleteStrategy() throws Exception {
        doNothing().when(aiEmployeeService).deleteStrategy(1L);

        mockMvc.perform(delete("/api/v1/ai-employees/1/strategies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
