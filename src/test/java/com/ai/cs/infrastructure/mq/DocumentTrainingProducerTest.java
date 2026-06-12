package com.ai.cs.infrastructure.mq;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DocumentTrainingProducerTest {

    @Test
    void shouldHaveCorrectTopic() {
        assertEquals("doc-training-topic", RocketMQConfig.DOC_TRAINING_TOPIC);
    }

    @Test
    void shouldSerializeMessagePayload() {
        DocumentTrainingProducer.DocumentMessage msg =
                new DocumentTrainingProducer.DocumentMessage(123L);
        assertEquals(123L, msg.documentId());
        assertNotNull(msg.timestamp());
    }

    @Test
    void shouldHandleNullDocumentId() {
        assertThrows(Exception.class, () ->
                new DocumentTrainingProducer.DocumentMessage(null));
    }
}
