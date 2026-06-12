package com.ai.cs.gateway.rest;

import com.ai.cs.domain.permission.SysUser;
import com.ai.cs.domain.permission.repository.SysUserRepository;
import com.ai.cs.shared.dto.ApiResponse;
import com.ai.cs.shared.security.JwtTokenProvider;
import com.ai.cs.shared.security.TicketService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TicketService ticketService;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        SysUser user = userRepository.findByUsername(request.getUsername())
                .orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ApiResponse.error(401, "用户名或密码错误");
        }
        if (!"ENABLED".equals(user.getStatus())) {
            return ApiResponse.error(403, "账号已被禁用");
        }

        List<String> permissions = getUserPermissions(user);
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), permissions);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("token", token);
        result.put("username", user.getUsername());
        result.put("roleCode", user.getRoleCode());
        result.put("agentId", user.getAgentId());
        result.put("permissions", permissions);
        return ApiResponse.success(result);
    }

    @PostMapping("/ticket")
    public ApiResponse<Map<String, Object>> getWebSocketTicket(
            @RequestAttribute(value = "userId", required = false) Long userId) {
        // userId from JWT filter context; fallback for test/dev
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        SysUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ApiResponse.error(401, "用户不存在");
        }
        String ticket = ticketService.createTicket(user.getAgentId(), user.getUsername());
        return ApiResponse.success(Map.of("ticket", ticket));
    }

    private List<String> getUserPermissions(SysUser user) {
        return switch (user.getRoleCode()) {
            case "ADMIN" -> List.of("ai_employee:view", "ai_employee:edit", "knowledge:view", "knowledge:edit",
                    "agent:view", "agent:edit", "im:access", "dashboard:view");
            case "LEADER" -> List.of("ai_employee:view", "knowledge:view", "agent:view", "im:access", "dashboard:view");
            case "AGENT" -> List.of("im:access");
            default -> List.of();
        };
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
