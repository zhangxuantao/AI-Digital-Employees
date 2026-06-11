package com.ai.cs.gateway.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.websocket.Session;

public interface MessageHandler {
    String supportedType();
    void handle(JsonNode message, Session session, Long agentId);
}
