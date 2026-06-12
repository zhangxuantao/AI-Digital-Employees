package com.ai.cs.gateway.rest;

import com.ai.cs.application.aiemployee.AiEmployeeService;
import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.domain.employee.AiEmployeeReplyStrategy;
import com.ai.cs.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-employees")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ai_employee:view')")
public class AiEmployeeController {

    private final AiEmployeeService aiEmployeeService;

    @GetMapping
    public ApiResponse<List<AiEmployee>> list() {
        return ApiResponse.success(aiEmployeeService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<AiEmployee> get(@PathVariable Long id) {
        return ApiResponse.success(aiEmployeeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ai_employee:edit')")
    public ApiResponse<AiEmployee> create(@RequestBody AiEmployee employee) {
        return ApiResponse.success(aiEmployeeService.create(employee));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ai_employee:edit')")
    public ApiResponse<AiEmployee> update(@PathVariable Long id, @RequestBody AiEmployee employee) {
        return ApiResponse.success(aiEmployeeService.update(id, employee));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ai_employee:edit')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        aiEmployeeService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/{employeeId}/strategies")
    public ApiResponse<List<AiEmployeeReplyStrategy>> getStrategies(@PathVariable Long employeeId) {
        return ApiResponse.success(aiEmployeeService.getStrategies(employeeId));
    }

    @PostMapping("/{employeeId}/strategies")
    @PreAuthorize("hasAuthority('ai_employee:edit')")
    public ApiResponse<AiEmployeeReplyStrategy> saveStrategy(@PathVariable Long employeeId,
                                                               @RequestBody AiEmployeeReplyStrategy strategy) {
        return ApiResponse.success(aiEmployeeService.saveStrategy(employeeId, strategy));
    }

    @DeleteMapping("/{employeeId}/strategies/{strategyId}")
    @PreAuthorize("hasAuthority('ai_employee:edit')")
    public ApiResponse<Void> deleteStrategy(@PathVariable Long employeeId, @PathVariable Long strategyId) {
        aiEmployeeService.deleteStrategy(strategyId);
        return ApiResponse.success();
    }
}
