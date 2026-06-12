package com.ai.cs.domain.employee;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "ai_employee_account")
public class AiEmployeeAccount extends BaseEntity {

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "platform")
    private String platform;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "access_config", columnDefinition = "JSON")
    private String accessConfig;

    @Column(name = "status")
    private String status = "ENABLED";
}
