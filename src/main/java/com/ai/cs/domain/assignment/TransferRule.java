package com.ai.cs.domain.assignment;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "transfer_rule")
public class TransferRule extends BaseEntity {

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "trigger_type")
    private String triggerType;

    @Column(name = "trigger_config", columnDefinition = "JSON")
    private String triggerConfig;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "action_config", columnDefinition = "JSON")
    private String actionConfig;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "enabled", columnDefinition = "TINYINT NOT NULL DEFAULT 1")
    private Boolean enabled = true;
}
