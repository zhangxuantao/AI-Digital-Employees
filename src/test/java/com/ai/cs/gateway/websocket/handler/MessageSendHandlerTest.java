package com.ai.cs.gateway.websocket.handler;

import com.ai.cs.application.conversation.MessageService;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageSendHandlerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private Session session;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MessageSendHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MessageSendHandler(messageService, conversationRepository);
    }

    @Test
    void supportedTypeShouldReturnMessageResponse() {
        assertEquals("message.response", handler.supportedType());
    }

    @Test
    void handleShouldSaveMessageWhenConversationExists() throws Exception {
        when(conversationRepository.existsById(100L)).thenReturn(true);
        String json = "{\"type\":\"message.response\",\"data\":{\"conversationId\":100,\"content\":\"您好，有什么可以帮助您？\"}}";
        JsonNode node = objectMapper.readTree(json);

        handler.handle(node, session, 50L);

        verify(messageService).saveMessage(100L, "HUMAN", 50L, "您好，有什么可以帮助您？", "text", null);
    }

    @Test
    void handleShouldNotSaveMessageWhenConversationNotExists() throws Exception {
        when(conversationRepository.existsById(999L)).thenReturn(false);
        String json = "{\"type\":\"message.response\",\"data\":{\"conversationId\":999,\"content\":\"hello\"}}";
        JsonNode node = objectMapper.readTree(json);

        handler.handle(node, session, 50L);

        verify(messageService, never()).saveMessage(any(), any(), any(), any(), any(), any());
    }

    @Test
    void handleShouldUseDefaultMsgTypeWhenNotSpecified() throws Exception {
        when(conversationRepository.existsById(1L)).thenReturn(true);
        String json = "{\"type\":\"message.response\",\"data\":{\"conversationId\":1,\"content\":\"hello\"}}";
        JsonNode node = objectMapper.readTree(json);

        handler.handle(node, session, 50L);

        verify(messageService).saveMessage(1L, "HUMAN", 50L, "hello", "text", null);
    }
}
