package com.ai.cs.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
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
            SendResult result = rocketMQTemplate.syncSend(RocketMQConfig.DOC_TRAINING_TOPIC, payload);
            log.info("文档训练消息已发送: docId={}, msgId={}, status={}",
                    documentId, result.getMsgId(), result.getSendStatus());
        } catch (Exception e) {
            log.error("文档训练消息发送失败: docId={}", documentId, e);
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
