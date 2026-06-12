package com.ai.cs.gateway.websocket;

import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionRegistryTest {

    @Mock
    private Session session1;

    @Mock
    private Session session2;

    private SessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SessionRegistry();
    }

    @Test
    void registerShouldAddSessionAndTicket() {
        when(session1.getId()).thenReturn("session-1");
        when(session1.isOpen()).thenReturn(true);
        registry.register(1L, session1, "ticket-abc");

        assertTrue(registry.isOnline(1L));
        assertEquals(session1, registry.getSession(1L));
        assertEquals(1L, registry.getAgentId("session-1"));
    }

    @Test
    void registerShouldReplaceOldSession() {
        when(session1.getId()).thenReturn("session-1");
        when(session2.getId()).thenReturn("session-2");
        registry.register(1L, session1, "ticket-1");
        registry.register(1L, session2, "ticket-2");

        assertEquals(session2, registry.getSession(1L));
    }

    @Test
    void unregisterShouldRemoveSession() {
        when(session1.getId()).thenReturn("session-1");
        registry.register(1L, session1, "ticket-abc");
        registry.unregister(1L);

        assertFalse(registry.isOnline(1L));
        assertNull(registry.getSession(1L));
        assertNull(registry.getAgentId("session-1"));
    }

    @Test
    void isOnlineShouldReturnFalseForUnknownAgent() {
        assertFalse(registry.isOnline(999L));
    }

    @Test
    void getSessionShouldReturnNullForUnknownAgent() {
        assertNull(registry.getSession(999L));
    }

    @Test
    void getOnlineAgentIdsShouldReturnAllOnlineAgents() {
        when(session1.getId()).thenReturn("session-1");
        when(session2.getId()).thenReturn("session-2");
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        registry.register(1L, session1, "ticket-1");
        registry.register(2L, session2, "ticket-2");

        var onlineIds = registry.getOnlineAgentIds();

        assertTrue(onlineIds.contains(1L));
        assertTrue(onlineIds.contains(2L));
        assertEquals(2, onlineIds.size());
    }

    @Test
    void getOnlineAgentIdsShouldExcludeOfflineSessions() {
        when(session1.getId()).thenReturn("session-1");
        when(session2.getId()).thenReturn("session-2");
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(false);
        registry.register(1L, session1, "ticket-1");
        registry.register(2L, session2, "ticket-2");

        var onlineIds = registry.getOnlineAgentIds();

        assertTrue(onlineIds.contains(1L));
        assertFalse(onlineIds.contains(2L));
        assertEquals(1, onlineIds.size());
    }
}
