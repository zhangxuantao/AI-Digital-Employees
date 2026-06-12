package com.ai.cs.gateway.websocket;

import com.ai.cs.gateway.websocket.handler.MessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageDispatcherTest {

    @Mock
    private MessageHandler handler1;

    @Mock
    private MessageHandler handler2;

    @Mock
    private Session session;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MessageDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        when(handler1.supportedType()).thenReturn("message.response");
        when(handler2.supportedType()).thenReturn("ping.request");
        dispatcher = new MessageDispatcher(objectMapper, List.of(handler1, handler2));
    }

    @Test
    void dispatchShouldRouteToCorrectHandler() {
        String json = "{\"type\":\"message.response\",\"data\":{\"conversationId\":1,\"content\":\"hello\"}}";

        dispatcher.dispatch(json, session, 1L);

        verify(handler1).handle(any(JsonNode.class), eq(session), eq(1L));
        verify(handler2, never()).handle(any(), any(), any());
    }

    @Test
    void dispatchShouldRoutePingToHeartbeatHandler() {
        String json = "{\"type\":\"ping.request\",\"data\":{}}";

        dispatcher.dispatch(json, session, 1L);

        verify(handler2).handle(any(JsonNode.class), eq(session), eq(1L));
        verify(handler1, never()).handle(any(), any(), any());
    }

    @Test
    void dispatchShouldNotThrowForUnknownType() {
        String json = "{\"type\":\"unknown.type\",\"data\":{}}";

        dispatcher.dispatch(json, session, 1L);

        verify(handler1, never()).handle(any(), any(), any());
        verify(handler2, never()).handle(any(), any(), any());
    }

    @Test
    void dispatchShouldNotThrowForInvalidJson() {
        dispatcher.dispatch("not-json", session, 1L);

        verify(handler1, never()).handle(any(), any(), any());
        verify(handler2, never()).handle(any(), any(), any());
    }
}
