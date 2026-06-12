package com.ai.cs.infrastructure.mq;

import com.ai.cs.application.knowledge.DocumentTrainingService;
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
    topic = RocketMQConfig.DOC_TRAINING_TOPIC,
    consumerGroup = "${rocketmq.consumer.group:cs-consumer-group}"
)
public class DocumentTrainingConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final DocumentTrainingService trainingService;

    @Override
    public void onMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            Long documentId = root.path("documentId").asLong();
            if (documentId == null || documentId == 0) {
                log.warn("无效的训练消息，缺少documentId: {}", message);
                return;
            }
            log.info("收到文档训练消息: docId={}", documentId);
            trainingService.train(documentId);
        } catch (Exception e) {
            log.error("消费文档训练消息失败: message={}", message, e);
            throw new RuntimeException("文档训练消费失败", e);
        }
    }
}
