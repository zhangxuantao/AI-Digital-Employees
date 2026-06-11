package com.ai.cs.gateway.websocket.handler;

import com.ai.cs.application.conversation.MessageService;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSendHandler implements MessageHandler {
    private final MessageService messageService;
    private final ConversationRepository conversationRepository;

    @Override
    public String supportedType() { return "message.response"; }

    @Override
    public void handle(JsonNode message, Session session, Long agentId) {
        Long conversationId = message.path("data").path("conversationId").asLong();
        String content = message.path("data").path("content").asText();
        String msgType = message.path("data").path("msgType").asText("text");
        if (!conversationRepository.existsById(conversationId)) {
            log.warn("会话不存在: {}", conversationId);
            return;
        }
        messageService.saveMessage(conversationId, "HUMAN", agentId, content, msgType, null);
    }
}
