package com.ai.cs.gateway.websocket;

import com.ai.cs.shared.security.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@ServerEndpoint("/ws/chat/{ticket}")
public class ChatWebSocketServer {

    private static SessionRegistry sessionRegistry;
    private static TicketService ticketService;
    private static MessageDispatcher messageDispatcher;
    private static ObjectMapper objectMapper;
    private Long agentId;

    public ChatWebSocketServer(SessionRegistry sessionRegistry, TicketService ticketService,
                                MessageDispatcher messageDispatcher, ObjectMapper objectMapper) {
        ChatWebSocketServer.sessionRegistry = sessionRegistry;
        ChatWebSocketServer.ticketService = ticketService;
        ChatWebSocketServer.messageDispatcher = messageDispatcher;
        ChatWebSocketServer.objectMapper = objectMapper;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("ticket") String ticket) {
        var ticketInfo = ticketService.validate(ticket);
        if (ticketInfo == null) {
            sendAndClose(session, Map.of("type", "auth.failed", "code", 401, "data", Map.of("message", "ticket无效或过期")));
            return;
        }
        this.agentId = ticketInfo.agentId();
        sessionRegistry.register(agentId, session, ticket);
        sendMessage(session, Map.of("type", "auth.success", "data", Map.of("agentId", agentId)));
        log.info("客服WebSocket认证成功: agentId={}", agentId);
    }

    @OnMessage
    public void onMessage(String text, Session session) {
        messageDispatcher.dispatch(text, session, agentId);
    }

    @OnClose
    public void onClose() {
        if (agentId != null) sessionRegistry.unregister(agentId);
    }

    @OnError
    public void onError(Throwable t) {
        log.error("客服WebSocket错误: agentId={}", agentId, t);
    }

    private void sendMessage(Session session, Object msg) {
        try { session.getBasicRemote().sendText(objectMapper.writeValueAsString(msg)); } catch (Exception e) { log.error("发送失败", e); }
    }

    private void sendAndClose(Session session, Object msg) {
        try { session.getBasicRemote().sendText(objectMapper.writeValueAsString(msg)); session.close(); } catch (Exception ignored) {}
    }
}
