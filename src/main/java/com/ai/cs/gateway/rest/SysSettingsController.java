package com.ai.cs.gateway.rest;

import com.ai.cs.domain.permission.SysPermission;
import com.ai.cs.domain.permission.SysRole;
import com.ai.cs.domain.permission.SysUser;
import com.ai.cs.domain.permission.repository.SysPermissionRepository;
import com.ai.cs.domain.permission.repository.SysRoleRepository;
import com.ai.cs.domain.permission.repository.SysUserRepository;
import com.ai.cs.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ai_employee:view')")
public class SysSettingsController {

    private final SysUserRepository userRepo;
    private final SysRoleRepository roleRepo;
    private final SysPermissionRepository permRepo;

    @GetMapping("/users")
    public ApiResponse<List<SysUser>> listUsers() {
        return ApiResponse.success(userRepo.findAll());
    }

    @GetMapping("/roles")
    public ApiResponse<List<SysRole>> listRoles() {
        return ApiResponse.success(roleRepo.findAll());
    }

    @GetMapping("/permissions")
    public ApiResponse<List<SysPermission>> listPermissions() {
        return ApiResponse.success(permRepo.findAll());
    }
}
