package com.ai.cs.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
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
            // Use syncSend to send raw JSON string directly, avoiding converter wrapping
            byte[] body = payload.getBytes(StandardCharsets.UTF_8);
            Message message = new Message(RocketMQConfig.DOC_TRAINING_TOPIC, body);
            SendResult result = rocketMQTemplate.getProducer().send(message);
            log.info("文档训练消息已发送: docId={}, msgId={}, status={}",
                    documentId, result.getMsgId(), result.getSendStatus());
        } catch (Exception e) {
            log.error("文档训练消息发送失败: docId={}, 训练将同步执行", documentId, e);
            // Fallback: 如果 RocketMQ 不可用，警告但不中断上传流程
            // 文档状态保持 UPLOADED，可通过后台手动触发训练
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
