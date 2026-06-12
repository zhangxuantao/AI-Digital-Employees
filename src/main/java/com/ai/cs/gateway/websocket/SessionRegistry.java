package com.ai.cs.gateway.websocket;

import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SessionRegistry {
    private final Map<Long, Session> agentSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionToAgent = new ConcurrentHashMap<>();
    private final Map<Long, String> agentTickets = new ConcurrentHashMap<>();

    public void register(Long agentId, Session session, String ticket) {
        Session old = agentSessions.put(agentId, session);
        if (old != null && old.isOpen()) {
            try { old.close(); } catch (Exception ignored) {}
        }
        sessionToAgent.put(session.getId(), agentId);
        agentTickets.put(agentId, ticket);
        log.info("客服上线: agentId={}", agentId);
    }

    public void unregister(Long agentId) {
        Session session = agentSessions.remove(agentId);
        if (session != null) sessionToAgent.remove(session.getId());
        agentTickets.remove(agentId);
        log.info("客服下线: agentId={}", agentId);
    }

    public Session getSession(Long agentId) { return agentSessions.get(agentId); }
    public Long getAgentId(String sessionId) { return sessionToAgent.get(sessionId); }

    public Set<Long> getOnlineAgentIds() {
        return agentSessions.keySet().stream()
                .filter(id -> agentSessions.get(id).isOpen())
                .collect(Collectors.toSet());
    }

    public boolean isOnline(Long agentId) {
        Session session = agentSessions.get(agentId);
        return session != null && session.isOpen();
    }

    /**
     * 向指定客服推送消息
     */
    public void sendToAgent(Long agentId, String message) {
        Session session = agentSessions.get(agentId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                log.warn("推送消息失败: agentId={}", agentId, e);
                unregister(agentId);
            }
        }
    }

    /**
     * 向所有在线客服广播消息
     */
    public void broadcast(String message) {
        for (Long agentId : getOnlineAgentIds()) {
            sendToAgent(agentId, message);
        }
    }

    /**
     * 向所有状态为ONLINE的客服广播
     */
    public void broadcastToOnline(String message) {
        broadcast(message);
    }
}
