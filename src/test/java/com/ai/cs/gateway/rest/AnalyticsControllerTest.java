package com.ai.cs.gateway.rest;

import com.ai.cs.application.analytics.AiEmployeeAnalyticsService;
import com.ai.cs.application.analytics.ServiceAnalyticsService;
import com.ai.cs.application.analytics.TopQuestionService;
import com.ai.cs.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(authorities = "dashboard:view")
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiEmployeeAnalyticsService employeeAnalytics;

    @MockBean
    private ServiceAnalyticsService serviceAnalytics;

    @MockBean
    private TopQuestionService topQuestionService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void getEmployeeStats_shouldReturnStats() throws Exception {
        AiEmployeeAnalyticsService.EmployeeStats stats = new AiEmployeeAnalyticsService.EmployeeStats();
        stats.setTotalConversations(10);
        stats.setClosedConversations(8);
        stats.setTransferredConversations(2);
        stats.setTransferRate(0.2);
        stats.setResolutionRate(0.6);

        when(employeeAnalytics.getEmployeeStats(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(stats);

        mockMvc.perform(get("/api/v1/analytics/employee/1")
                        .param("start", "2024-01-01T00:00:00")
                        .param("end", "2024-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalConversations").value(10))
                .andExpect(jsonPath("$.data.closedConversations").value(8))
                .andExpect(jsonPath("$.data.transferredConversations").value(2))
                .andExpect(jsonPath("$.data.transferRate").value(0.2))
                .andExpect(jsonPath("$.data.resolutionRate").value(0.6));
    }

    @Test
    void getRealtimeStats_shouldReturnStats() throws Exception {
        ServiceAnalyticsService.RealtimeStats stats = new ServiceAnalyticsService.RealtimeStats();
        stats.setActiveConversations(5);
        stats.setWaitingCount(3);
        stats.setQueuedCount(2);
        stats.setTotalOnline(10);

        when(serviceAnalytics.getRealtimeStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/analytics/service/realtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.activeConversations").value(5))
                .andExpect(jsonPath("$.data.waitingCount").value(3))
                .andExpect(jsonPath("$.data.queuedCount").value(2))
                .andExpect(jsonPath("$.data.totalOnline").value(10));
    }

    @Test
    void getTopQuestions_shouldReturnQuestions() throws Exception {
        when(topQuestionService.getTopQuestions(5))
                .thenReturn(List.of(
                        new TopQuestionService.QuestionRank("问题1", 100),
                        new TopQuestionService.QuestionRank("问题2", 50)
                ));

        mockMvc.perform(get("/api/v1/analytics/questions/top")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].question").value("问题1"))
                .andExpect(jsonPath("$.data[1].count").value(50));
    }

    @Test
    void getOverview_shouldReturnOverview() throws Exception {
        ServiceAnalyticsService.RealtimeStats realtime = new ServiceAnalyticsService.RealtimeStats();
        realtime.setActiveConversations(5);
        realtime.setTotalOnline(10);

        when(serviceAnalytics.getRealtimeStats()).thenReturn(realtime);
        when(topQuestionService.getTopQuestions(5)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.realtime.activeConversations").value(5))
                .andExpect(jsonPath("$.data.topQuestions").isArray());
    }
}
