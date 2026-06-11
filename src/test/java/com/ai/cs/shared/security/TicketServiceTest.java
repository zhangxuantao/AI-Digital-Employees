package com.ai.cs.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TicketServiceTest {

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService();
    }

    @Test
    void createAndValidateTicketShouldSucceed() {
        String ticket = ticketService.createTicket(1L, "testagent");
        assertNotNull(ticket);
        assertFalse(ticket.isEmpty());

        TicketService.TicketInfo info = ticketService.validate(ticket);
        assertNotNull(info);
        assertEquals(1L, info.agentId());
        assertEquals("testagent", info.username());
    }

    @Test
    void validateInvalidTicketShouldReturnNull() {
        TicketService.TicketInfo info = ticketService.validate("non-existent-ticket");
        assertNull(info);
    }

    @Test
    void revokeTicketShouldMakeItInvalid() {
        String ticket = ticketService.createTicket(1L, "testagent");
        ticketService.revoke(ticket);
        assertNull(ticketService.validate(ticket));
    }

    @Test
    void multipleTicketsShouldBeIndependent() {
        String ticket1 = ticketService.createTicket(1L, "agent1");
        String ticket2 = ticketService.createTicket(2L, "agent2");

        TicketService.TicketInfo info1 = ticketService.validate(ticket1);
        TicketService.TicketInfo info2 = ticketService.validate(ticket2);

        assertEquals(1L, info1.agentId());
        assertEquals(2L, info2.agentId());
    }
}
