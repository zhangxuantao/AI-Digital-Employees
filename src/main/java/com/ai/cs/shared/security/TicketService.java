package com.ai.cs.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class TicketService {
    private final Map<String, TicketInfo> ticketStore = new ConcurrentHashMap<>();

    public String createTicket(Long agentId, String username) {
        String ticket = UUID.randomUUID().toString().replace("-", "");
        ticketStore.put(ticket, new TicketInfo(agentId, username, System.currentTimeMillis()));
        return ticket;
    }

    public TicketInfo validate(String ticket) {
        TicketInfo info = ticketStore.get(ticket);
        if (info == null) return null;
        if (System.currentTimeMillis() - info.createdAt > 7200000) {
            ticketStore.remove(ticket);
            return null;
        }
        return info;
    }

    public void revoke(String ticket) {
        ticketStore.remove(ticket);
    }

    public record TicketInfo(Long agentId, String username, long createdAt) {}
}
