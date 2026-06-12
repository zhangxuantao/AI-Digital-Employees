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
}
