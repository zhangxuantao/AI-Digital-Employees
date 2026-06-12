package com.ai.cs.domain.employee;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "ai_employee_reply_strategy")
public class AiEmployeeReplyStrategy extends BaseEntity {

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "strategy_type")
    private String strategyType;

    @Column(name = "config_json", columnDefinition = "JSON")
    private String configJson;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
