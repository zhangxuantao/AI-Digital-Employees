package com.ai.cs.infrastructure.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true", matchIfMissing = true)
public class AssignmentNotificationProducer {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 分配完成后发送通知消息
     */
    public void send(Long agentId, Long conversationId) {
        AssignNotifyMessage msg = new AssignNotifyMessage(agentId, conversationId);
        try {
            String payload = objectMapper.writeValueAsString(msg);
            rocketMQTemplate.convertAndSend(RocketMQConfig.ASSIGNMENT_NOTIFY_TOPIC, payload);
            log.info("分配通知已发送: agentId={}, convId={}", agentId, conversationId);
        } catch (JsonProcessingException e) {
            log.error("分配通知序列化失败: agentId={}", agentId, e);
        }
    }

    public record AssignNotifyMessage(Long agentId, Long conversationId, LocalDateTime timestamp) {
        public AssignNotifyMessage(Long agentId, Long conversationId) {
            this(agentId, conversationId, LocalDateTime.now());
        }
    }
}
