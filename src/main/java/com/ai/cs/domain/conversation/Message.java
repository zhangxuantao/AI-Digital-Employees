package com.ai.cs.domain.conversation;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "message")
public class Message extends BaseEntity {

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "sender_type")
    private String senderType;

    @Column(name = "sender_id")
    private String senderId;

    @Column(name = "msg_type")
    private String msgType = "text";

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "send_time")
    private LocalDateTime sendTime = LocalDateTime.now();
}
