package com.ai.cs.gateway.rest;

import com.ai.cs.domain.assignment.AgentChannelPermission;
import com.ai.cs.domain.assignment.repository.AgentChannelPermissionRepository;
import com.ai.cs.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agent-permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('agent:view')")
public class AgentPermissionController {

    private final AgentChannelPermissionRepository permissionRepo;

    @GetMapping
    public ApiResponse<List<AgentChannelPermission>> list(@RequestParam Long agentId) {
        return ApiResponse.success(permissionRepo.findByAgentId(agentId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('agent:edit')")
    public ApiResponse<AgentChannelPermission> create(@RequestBody AgentChannelPermission perm) {
        return ApiResponse.success(permissionRepo.save(perm));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('agent:edit')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionRepo.deleteById(id);
        return ApiResponse.success();
    }
}
