package com.ai.cs.domain.knowledge;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "knowledge_base")
public class KnowledgeBase extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "employee_id")
    private Long employeeId;
}
