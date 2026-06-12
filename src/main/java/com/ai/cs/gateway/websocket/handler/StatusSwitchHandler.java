package com.ai.cs.gateway.websocket.handler;

import com.ai.cs.domain.assignment.HumanAgent;
import com.ai.cs.domain.assignment.repository.HumanAgentRepository;
import com.ai.cs.gateway.websocket.SessionRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatusSwitchHandler implements MessageHandler {

    private final HumanAgentRepository agentRepository;
    private final SessionRegistry sessionRegistry;

    @Override
    public String supportedType() { return "status_switch"; }

    @Override
    public void handle(JsonNode payload, Session session, Long agentId) {
        String status = payload.path("status").asText();
        if (status == null || status.isEmpty()) {
            log.warn("状态切换参数缺失: agentId={}", agentId);
            return;
        }

        if (!List.of("ONLINE", "BUSY", "OFFLINE").contains(status)) {
            log.warn("无效的状态值: {}", status);
            return;
        }

        HumanAgent agent = agentRepository.findById(agentId).orElse(null);
        if (agent == null) {
            log.warn("客服不存在: agentId={}", agentId);
            return;
        }

        agent.setStatus(status);
        agentRepository.save(agent);

        // Broadcast status change
        String broadcastJson = String.format(
            "{\"type\":\"status_change\",\"agentId\":%d,\"agentName\":\"%s\",\"status\":\"%s\"}",
            agentId, agent.getName(), status);
        sessionRegistry.broadcast(broadcastJson);

        log.info("客服状态切换: agentId={}, status={}", agentId, status);

        if ("OFFLINE".equals(status)) {
            sessionRegistry.broadcastToOnline("{\"type\":\"reassign_needed\",\"message\":\"客服离线，请检查待分配队列\"}");
        }
    }
}
