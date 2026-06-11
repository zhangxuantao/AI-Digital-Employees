package com.ai.cs.gateway.rest;

import com.ai.cs.application.analytics.AiEmployeeAnalyticsService;
import com.ai.cs.application.analytics.ServiceAnalyticsService;
import com.ai.cs.application.analytics.TopQuestionService;
import com.ai.cs.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('dashboard:view')")
public class AnalyticsController {

    private final AiEmployeeAnalyticsService employeeAnalytics;
    private final ServiceAnalyticsService serviceAnalytics;
    private final TopQuestionService topQuestionService;

    @GetMapping("/employee/{employeeId}")
    public ApiResponse<AiEmployeeAnalyticsService.EmployeeStats> getEmployeeStats(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ApiResponse.success(employeeAnalytics.getEmployeeStats(employeeId, start, end));
    }

    @GetMapping("/service/realtime")
    public ApiResponse<ServiceAnalyticsService.RealtimeStats> getRealtimeStats() {
        return ApiResponse.success(serviceAnalytics.getRealtimeStats());
    }

    @GetMapping("/questions/top")
    public ApiResponse<?> getTopQuestions(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(topQuestionService.getTopQuestions(limit));
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getOverview() {
        var realtime = serviceAnalytics.getRealtimeStats();
        return ApiResponse.success(Map.of(
                "realtime", realtime,
                "topQuestions", topQuestionService.getTopQuestions(5)
        ));
    }
}
