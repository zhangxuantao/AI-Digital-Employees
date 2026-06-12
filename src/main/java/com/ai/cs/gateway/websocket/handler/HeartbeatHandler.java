package com.ai.cs.gateway.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatHandler implements MessageHandler {
    private final ObjectMapper objectMapper;

    @Override
    public String supportedType() { return "ping.request"; }

    @Override
    public void handle(JsonNode message, Session session, Long agentId) {
        try {
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(Map.of("type", "pong.response", "code", 0)));
        } catch (Exception e) {
            log.error("心跳响应失败", e);
        }
    }
}
