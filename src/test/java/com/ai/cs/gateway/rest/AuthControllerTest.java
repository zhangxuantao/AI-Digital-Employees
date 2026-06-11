package com.ai.cs.gateway.rest;

import com.ai.cs.domain.permission.SysUser;
import com.ai.cs.domain.permission.repository.SysUserRepository;
import com.ai.cs.shared.security.JwtTokenProvider;
import com.ai.cs.shared.security.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SysUserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TicketService ticketService;

    @Test
    void login_shouldReturnTokenOnValidCredentials() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash("encoded-password");
        user.setRoleCode("AGENT");
        user.setStatus("ENABLED");
        user.setAgentId(100L);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.createToken(anyLong(), anyString(), anyList()))
                .thenReturn("test-jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.roleCode").value("AGENT"))
                .andExpect(jsonPath("$.data.agentId").value(100));
    }

    @Test
    void login_shouldReturn401OnInvalidPassword() throws Exception {
        SysUser user = new SysUser();
        user.setUsername("testuser");
        user.setPasswordHash("encoded-password");
        user.setStatus("ENABLED");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void login_shouldReturn401OnNonExistentUser() throws Exception {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"unknown\",\"password\":\"any\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void login_shouldReturn403OnDisabledAccount() throws Exception {
        SysUser user = new SysUser();
        user.setUsername("disableduser");
        user.setPasswordHash("encoded-password");
        user.setStatus("DISABLED");

        when(userRepository.findByUsername("disableduser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"disableduser\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("账号已被禁用"));
    }

    @Test
    void login_shouldReturnAdminPermissions() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPasswordHash("encoded");
        user.setRoleCode("ADMIN");
        user.setStatus("ENABLED");
        user.setAgentId(0L);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
        when(jwtTokenProvider.createToken(anyLong(), anyString(), anyList()))
                .thenReturn("admin-jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.permissions.length()").value(8));
    }

    @Test
    void ticket_shouldReturnTicketOnValidRequest() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("testagent");
        user.setAgentId(100L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(ticketService.createTicket(100L, "testagent")).thenReturn("test-ticket-123");

        mockMvc.perform(post("/api/v1/auth/ticket")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ticket").value("test-ticket-123"));
    }

    @Test
    void ticket_shouldReturn401WhenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/v1/auth/ticket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    void ticket_shouldReturn401WhenUserNotFound() throws Exception {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/ticket")
                        .requestAttr("userId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }
}
