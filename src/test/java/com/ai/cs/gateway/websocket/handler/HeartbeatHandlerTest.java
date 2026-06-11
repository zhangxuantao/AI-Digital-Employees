package com.ai.cs.gateway.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeartbeatHandlerTest {

    @Mock
    private Session session;

    @Mock
    private RemoteEndpoint.Basic basicRemote;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HeartbeatHandler handler;

    @BeforeEach
    void setUp() {
        handler = new HeartbeatHandler(objectMapper);
    }

    @Test
    void supportedTypeShouldReturnPingRequest() {
        assertEquals("ping.request", handler.supportedType());
    }

    @Test
    void handleShouldSendPongResponse() throws Exception {
        when(session.getBasicRemote()).thenReturn(basicRemote);

        handler.handle(null, session, 1L);

        verify(basicRemote).sendText(argThat(json ->
                json.contains("\"type\":\"pong.response\"") &&
                json.contains("\"code\":0")));
    }
}
