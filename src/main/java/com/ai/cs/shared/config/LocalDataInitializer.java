package com.ai.cs.shared.config;

import com.ai.cs.domain.assignment.HumanAgent;
import com.ai.cs.domain.assignment.repository.HumanAgentRepository;
import com.ai.cs.domain.permission.SysRole;
import com.ai.cs.domain.permission.SysUser;
import com.ai.cs.domain.permission.repository.SysRoleRepository;
import com.ai.cs.domain.permission.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements CommandLineRunner {

    private final SysRoleRepository roleRepository;
    private final SysUserRepository userRepository;
    private final HumanAgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            log.info("Local data already initialized, skipping");
            return;
        }

        log.info("Initializing local dev data...");

        roleRepository.save(createRole("ADMIN", "超级管理员", "全部菜单 + 全部数据"));
        roleRepository.save(createRole("LEADER", "客服主管", "客服管理 + IM工作台 + 数据看板"));
        roleRepository.save(createRole("AGENT", "客服", "IM工作台"));

        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRoleCode("ADMIN");
        admin.setStatus("ENABLED");
        userRepository.save(admin);

        HumanAgent agent = new HumanAgent();
        agent.setName("客服小王");
        agent.setPhone("13800138000");
        agent.setPasswordHash(passwordEncoder.encode("13800138000"));
        agent.setStatus("ONLINE");
        agent.setCurrentLoad(0);
        agent.setMaxConcurrent(5);
        agentRepository.save(agent);

        log.info("Local dev data initialized. Admin: admin/admin123, Agent: 客服小王");
    }

    private SysRole createRole(String code, String name, String description) {
        SysRole role = new SysRole();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        return role;
    }
}
