package com.ai.cs.gateway.rest;

import com.ai.cs.domain.assignment.AssignmentStrategyConfig;
import com.ai.cs.domain.assignment.repository.AssignmentStrategyConfigRepository;
import com.ai.cs.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/assignment-strategy")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ai_employee:view')")
public class AssignmentStrategyController {

    private final AssignmentStrategyConfigRepository strategyRepo;

    @GetMapping
    public ApiResponse<String> get(@RequestParam(required = false) Long employeeId) {
        if (employeeId != null) {
            return strategyRepo.findByEmployeeIdAndIsActive(employeeId, true)
                    .stream().findFirst()
                    .map(s -> ApiResponse.success(s.getStrategyType()))
                    .orElse(ApiResponse.success("ROUND_ROBIN"));
        }
        // No employeeId specified: return first active strategy found or default
        return ApiResponse.success("ROUND_ROBIN");
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ai_employee:edit')")
    public ApiResponse<Void> update(@RequestParam(required = false) Long employeeId,
                                     @RequestBody Map<String, String> body) {
        String strategy = body.getOrDefault("strategy", "ROUND_ROBIN");
        if (employeeId != null) {
            var existing = strategyRepo.findByEmployeeIdAndIsActive(employeeId, true);
            if (!existing.isEmpty()) {
                var config = existing.get(0);
                config.setStrategyType(strategy);
                strategyRepo.save(config);
            } else {
                var config = new AssignmentStrategyConfig();
                config.setEmployeeId(employeeId);
                config.setStrategyType(strategy);
                config.setIsActive(true);
                config.setConfigJson("{}");
                strategyRepo.save(config);
            }
        }
        return ApiResponse.success();
    }
}
