package com.ai.cs.domain.employee;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter @Setter
@Entity
@Table(name = "ai_employee")
public class AiEmployee extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "greeting_msg", columnDefinition = "TEXT")
    private String greetingMsg;

    @Column(name = "style")
    private String style = "PROFESSIONAL";

    @Column(name = "reply_length")
    private String replyLength = "MEDIUM";

    @Column(name = "content_check", columnDefinition = "JSON")
    private String contentCheck;

    @Column(name = "aggregate_interval")
    private Integer aggregateInterval = 3;

    @Column(name = "delay_interval")
    private Integer delayInterval = 2;

    @Column(name = "service_time_start")
    private LocalTime serviceTimeStart;

    @Column(name = "service_time_end")
    private LocalTime serviceTimeEnd;

    @Column(name = "weekdays")
    private String weekdays;

    @Column(name = "company_intro", columnDefinition = "TEXT NOT NULL")
    private String companyIntro;

    @Column(name = "product_intro", columnDefinition = "TEXT NOT NULL")
    private String productIntro;

    @Column(name = "service_scope", columnDefinition = "TEXT")
    private String serviceScope;

    @Column(name = "status")
    private String status = "ENABLED";
}
