package com.ai.cs.domain.assignment;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "agent_channel_permission")
public class AgentChannelPermission extends BaseEntity {

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "platform")
    private String platform;

    @Column(name = "employee_id")
    private Long employeeId;
}
