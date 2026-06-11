package com.ai.cs.domain.assignment;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "assignment_strategy")
public class AssignmentStrategyConfig extends BaseEntity {

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "strategy_type")
    private String strategyType;

    @Column(name = "config_json", columnDefinition = "JSON")
    private String configJson;

    @Column(name = "is_active")
    private Boolean isActive = false;
}
