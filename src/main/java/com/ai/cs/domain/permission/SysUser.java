package com.ai.cs.domain.permission;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "sys_user")
public class SysUser extends BaseEntity {

    @Column(name = "username")
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "role_code")
    private String roleCode;

    @Column(name = "status")
    private String status = "ENABLED";
}
