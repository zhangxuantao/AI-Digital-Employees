package com.ai.cs.infrastructure.mq;

import com.ai.cs.gateway.websocket.SessionRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true", matchIfMissing = true)
@RocketMQMessageListener(
    topic = RocketMQConfig.ASSIGNMENT_NOTIFY_TOPIC,
    consumerGroup = "${rocketmq.consumer.group:cs-consumer-group}"
)
public class AssignmentNotificationConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final SessionRegistry sessionRegistry;

    @Override
    public void onMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            Long agentId = root.path("agentId").asLong();
            Long conversationId = root.path("conversationId").asLong();

            if (agentId == null || agentId == 0) {
                log.warn("无效的分配通知，缺少agentId: {}", message);
                return;
            }

            String notifyJson = String.format(
                "{\"type\":\"new_assignment\",\"conversationId\":%d,\"message\":\"您有新的会话分配\"}",
                conversationId);
            sessionRegistry.sendToAgent(agentId, notifyJson);
            log.info("已推送分配通知: agentId={}, convId={}", agentId, conversationId);
        } catch (Exception e) {
            log.error("消费分配通知失败: message={}", message, e);
            throw new RuntimeException("分配通知消费失败", e);
        }
    }
}
