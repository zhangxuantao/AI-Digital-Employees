package com.ai.cs.gateway.rest;

import com.ai.cs.domain.assignment.HumanAgent;
import com.ai.cs.domain.assignment.repository.HumanAgentRepository;
import com.ai.cs.shared.dto.ApiResponse;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('agent:view')")
public class HumanAgentController {

    private final HumanAgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ApiResponse<List<HumanAgent>> list() { return ApiResponse.success(agentRepository.findAll()); }

    @GetMapping("/{id}")
    public ApiResponse<HumanAgent> get(@PathVariable Long id) { return ApiResponse.success(agentRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND))); }

    @PostMapping
    @PreAuthorize("hasAuthority('agent:edit')")
    public ApiResponse<HumanAgent> create(@RequestBody HumanAgent agent) {
        agent.setPasswordHash(passwordEncoder.encode(agent.getPhone()));
        agent.setStatus("OFFLINE");
        agent.setCurrentLoad(0);
        if (agent.getMaxConcurrent() == null) agent.setMaxConcurrent(5);
        return ApiResponse.success(agentRepository.save(agent));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('agent:edit')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        agentRepository.deleteById(id);
        return ApiResponse.success();
    }
}
