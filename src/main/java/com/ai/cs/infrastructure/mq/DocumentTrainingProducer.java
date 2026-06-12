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
public class DocumentTrainingProducer {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    public void send(Long documentId) {
        DocumentMessage msg = new DocumentMessage(documentId);
        try {
            String payload = objectMapper.writeValueAsString(msg);
            rocketMQTemplate.convertAndSend(RocketMQConfig.DOC_TRAINING_TOPIC, payload);
            log.info("文档训练消息已发送: docId={}", documentId);
        } catch (JsonProcessingException e) {
            log.error("文档训练消息序列化失败: docId={}", documentId, e);
            throw new RuntimeException("消息序列化失败", e);
        }
    }

    public record DocumentMessage(Long documentId, LocalDateTime timestamp) {
        public DocumentMessage {
            if (documentId == null) {
                throw new IllegalArgumentException("documentId must not be null");
            }
        }

        public DocumentMessage(Long documentId) {
            this(documentId, LocalDateTime.now());
        }
    }
}
