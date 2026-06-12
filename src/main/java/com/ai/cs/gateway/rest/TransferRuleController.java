package com.ai.cs.gateway.rest;

import com.ai.cs.domain.assignment.TransferRule;
import com.ai.cs.domain.assignment.repository.TransferRuleRepository;
import com.ai.cs.shared.dto.ApiResponse;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transfer-rules")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ai_employee:view')")
public class TransferRuleController {

    private final TransferRuleRepository ruleRepository;

    @GetMapping
    public ApiResponse<List<TransferRule>> list(@RequestParam(required = false) Long employeeId) {
        if (employeeId != null) {
            return ApiResponse.success(ruleRepository.findByEmployeeIdAndEnabledOrderByPriorityAsc(employeeId, true));
        }
        return ApiResponse.success(ruleRepository.findByEnabledOrderByPriorityAsc(true));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ai_employee:edit')")
    public ApiResponse<TransferRule> create(@RequestBody TransferRule rule) {
        if (rule.getEnabled() == null) rule.setEnabled(true);
        if (rule.getPriority() == null) rule.setPriority(0);
        return ApiResponse.success(ruleRepository.save(rule));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ai_employee:edit')")
    public ApiResponse<TransferRule> update(@PathVariable Long id, @RequestBody TransferRule update) {
        TransferRule rule = ruleRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.STRATEGY_NOT_FOUND));
        if (update.getTriggerType() != null) rule.setTriggerType(update.getTriggerType());
        if (update.getTriggerConfig() != null) rule.setTriggerConfig(update.getTriggerConfig());
        if (update.getActionType() != null) rule.setActionType(update.getActionType());
        if (update.getActionConfig() != null) rule.setActionConfig(update.getActionConfig());
        if (update.getPriority() != null) rule.setPriority(update.getPriority());
        if (update.getEnabled() != null) rule.setEnabled(update.getEnabled());
        return ApiResponse.success(ruleRepository.save(rule));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ai_employee:edit')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        TransferRule rule = ruleRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.STRATEGY_NOT_FOUND));
        rule.setEnabled(false);
        ruleRepository.save(rule);
        return ApiResponse.success();
    }
}
