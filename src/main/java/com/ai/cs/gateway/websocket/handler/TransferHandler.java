package com.ai.cs.gateway.websocket.handler;

import com.ai.cs.application.conversation.ConversationService;
import com.ai.cs.domain.permission.AuditLog;
import com.ai.cs.domain.permission.repository.AuditLogRepository;
import com.ai.cs.gateway.websocket.SessionRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferHandler implements MessageHandler {

    private final ConversationService conversationService;
    private final SessionRegistry sessionRegistry;
    private final AuditLogRepository auditLogRepository;

    @Override
    public String supportedType() { return "transfer"; }

    @Override
    public void handle(JsonNode payload, Session session, Long agentId) {
        Long conversationId = payload.path("conversationId").asLong();
        Long targetAgentId = payload.path("targetAgentId").asLong();

        if (conversationId == 0 || targetAgentId == 0) {
            log.warn("转接参数不完整: convId={}, targetId={}", conversationId, targetAgentId);
            sendError(session, "转接参数不完整");
            return;
        }

        try {
            conversationService.transferToHuman(conversationId, targetAgentId);

            // Notify target agent
            String notifyJson = String.format(
                "{\"type\":\"transfer_notify\",\"conversationId\":%d,\"message\":\"有会话转接给您\"}",
                conversationId);
            sessionRegistry.sendToAgent(targetAgentId, notifyJson);

            sendSuccess(session, "转接成功");

            // Audit log
            AuditLog logEntry = new AuditLog();
            logEntry.setUserId(agentId);
            logEntry.setAction("TRANSFER");
            logEntry.setTargetType("CONVERSATION");
            logEntry.setTargetId(String.valueOf(conversationId));
            logEntry.setDetail(String.format("{\"targetAgentId\":%d}", targetAgentId));
            auditLogRepository.save(logEntry);

            log.info("会话转接完成: convId={}, from={}, to={}", conversationId, agentId, targetAgentId);
        } catch (Exception e) {
            log.error("转接失败: convId={}", conversationId, e);
            sendError(session, "转接失败: " + e.getMessage());
        }
    }

    private void sendSuccess(Session session, String msg) {
        try {
            session.getBasicRemote().sendText(
                "{\"type\":\"transfer_result\",\"success\":true,\"message\":\"" + msg + "\"}");
        } catch (Exception ignored) {}
    }

    private void sendError(Session session, String msg) {
        try {
            session.getBasicRemote().sendText(
                "{\"type\":\"transfer_result\",\"success\":false,\"message\":\"" + msg + "\"}");
        } catch (Exception ignored) {}
    }
}
