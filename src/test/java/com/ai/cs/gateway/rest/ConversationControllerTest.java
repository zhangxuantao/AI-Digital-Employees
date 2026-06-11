package com.ai.cs.gateway.rest;

import com.ai.cs.application.conversation.ConversationService;
import com.ai.cs.application.conversation.MessageService;
import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.shared.security.JwtTokenProvider;
import com.ai.cs.domain.conversation.ConversationStatus;
import com.ai.cs.domain.conversation.Message;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ConversationController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(authorities = "im:access")
@ActiveProfiles("test")
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConversationRepository conversationRepository;

    @MockBean
    private ConversationService conversationService;

    @MockBean
    private MessageService messageService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void listConversations_shouldReturnAllConversations_whenNoRequestAttributes() throws Exception {
        Conversation conv = new Conversation();
        conv.setId(1L);
        conv.setCustomerId(10L);
        conv.setStatus(ConversationStatus.AI_ACTIVE);
        conv.setChannel("web");

        when(conversationRepository.findAll()).thenReturn(List.of(conv));

        mockMvc.perform(get("/api/v1/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].customerId").value(10))
                .andExpect(jsonPath("$.data[0].channel").value("web"));
    }

    @Test
    void listConversations_shouldReturnAllConversations_whenAdminRole() throws Exception {
        Conversation conv = new Conversation();
        conv.setId(1L);
        conv.setCustomerId(10L);
        conv.setStatus(ConversationStatus.AI_ACTIVE);

        when(conversationRepository.findAll()).thenReturn(List.of(conv));

        mockMvc.perform(get("/api/v1/conversations")
                        .requestAttr("roleCode", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void listConversations_shouldFilterByOwnerAgentId_whenAgentRole() throws Exception {
        Conversation conv = new Conversation();
        conv.setId(1L);
        conv.setCustomerId(10L);
        conv.setOwnerAgentId(5L);
        conv.setStatus(ConversationStatus.AI_ACTIVE);

        when(conversationRepository.findByOwnerAgentId(5L)).thenReturn(List.of(conv));

        mockMvc.perform(get("/api/v1/conversations")
                        .requestAttr("agentId", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void getMessages_shouldReturnMessagesForConversation() throws Exception {
        Message msg = new Message();
        msg.setId(1L);
        msg.setConversationId(1L);
        msg.setSenderType("CUSTOMER");
        msg.setContent("Hello");
        msg.setSendTime(LocalDateTime.now());

        when(messageService.getConversationMessages(1L)).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/v1/conversations/1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].content").value("Hello"))
                .andExpect(jsonPath("$.data[0].senderType").value("CUSTOMER"));
    }

    @Test
    void closeConversation_shouldCloseConversation() throws Exception {
        Conversation conv = new Conversation();
        conv.setId(1L);
        conv.setStatus(ConversationStatus.CLOSED);
        conv.setCloseReason("用户问题已解决");

        when(conversationService.closeConversation(eq(1L), anyString()))
                .thenReturn(conv);

        mockMvc.perform(post("/api/v1/conversations/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"用户问题已解决\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closeReason").value("用户问题已解决"));
    }

    @Test
    void closeConversation_shouldUseDefaultReasonWhenNotProvided() throws Exception {
        Conversation conv = new Conversation();
        conv.setId(2L);
        conv.setStatus(ConversationStatus.CLOSED);
        conv.setCloseReason("客服主动关闭");

        when(conversationService.closeConversation(eq(2L), anyString()))
                .thenReturn(conv);

        mockMvc.perform(post("/api/v1/conversations/2/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.closeReason").value("客服主动关闭"));
    }

    @Test
    void closeConversation_shouldHandleNullRequestBodyGracefully() throws Exception {
        Conversation conv = new Conversation();
        conv.setId(3L);
        conv.setStatus(ConversationStatus.CLOSED);
        conv.setCloseReason("客服主动关闭");

        when(conversationService.closeConversation(eq(3L), anyString()))
                .thenReturn(conv);

        mockMvc.perform(post("/api/v1/conversations/3/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
