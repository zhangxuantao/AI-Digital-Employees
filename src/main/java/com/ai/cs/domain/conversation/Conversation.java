package com.ai.cs.domain.conversation;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "conversation")
public class Conversation extends BaseEntity {

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "human_agent_id")
    private Long humanAgentId;

    @Column(name = "owner_agent_id")
    private Long ownerAgentId;

    @Column(name = "channel")
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ConversationStatus status;

    @Column(name = "start_time")
    private LocalDateTime startTime = LocalDateTime.now();

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "close_reason")
    private String closeReason;
}
