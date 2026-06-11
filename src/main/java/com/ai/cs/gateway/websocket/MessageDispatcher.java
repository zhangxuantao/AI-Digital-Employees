package com.ai.cs.gateway.websocket;

import com.ai.cs.gateway.websocket.handler.MessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MessageDispatcher {
    private final ObjectMapper objectMapper;
    private final Map<String, MessageHandler> handlerMap;

    public MessageDispatcher(ObjectMapper objectMapper, List<MessageHandler> handlers) {
        this.objectMapper = objectMapper;
        this.handlerMap = handlers.stream().collect(Collectors.toMap(MessageHandler::supportedType, Function.identity()));
    }

    public void dispatch(String text, Session session, Long agentId) {
        try {
            JsonNode root = objectMapper.readTree(text);
            String type = root.path("type").asText();
            MessageHandler handler = handlerMap.get(type);
            if (handler != null) {
                handler.handle(root, session, agentId);
            } else {
                log.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("消息分发异常", e);
        }
    }
}
