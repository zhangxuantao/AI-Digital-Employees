package com.ai.cs.domain.assignment;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "human_agent")
public class HumanAgent extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "status")
    private String status = "OFFLINE";

    @Column(name = "current_load")
    private Integer currentLoad = 0;

    @Column(name = "max_concurrent")
    private Integer maxConcurrent = 5;
}
