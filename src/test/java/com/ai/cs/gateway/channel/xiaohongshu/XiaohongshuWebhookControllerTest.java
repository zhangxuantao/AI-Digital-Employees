package com.ai.cs.gateway.channel.xiaohongshu;

import com.ai.cs.application.aiemployee.ReplyPipelineService;
import com.ai.cs.application.conversation.ConversationService;
import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.ConversationStatus;
import com.ai.cs.domain.customer.CustomerProfile;
import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.domain.employee.AiEmployeeAccount;
import com.ai.cs.domain.employee.repository.AiEmployeeAccountRepository;
import com.ai.cs.domain.employee.repository.AiEmployeeRepository;
import com.ai.cs.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = XiaohongshuWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class XiaohongshuWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private XiaohongshuChannelAdapter adapter;

    @MockBean
    private AiEmployeeAccountRepository accountRepo;

    @MockBean
    private AiEmployeeRepository employeeRepo;

    @MockBean
    private ConversationService conversationService;

    @MockBean
    private ReplyPipelineService replyPipelineService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void receiveMessage_shouldProcessAndReturnOk() throws Exception {
        CustomerProfile customer = new CustomerProfile();
        customer.setId(100L);

        AiEmployeeAccount account = new AiEmployeeAccount();
        account.setId(1L);
        account.setEmployeeId(10L);

        AiEmployee employee = new AiEmployee();
        employee.setId(10L);
        employee.setStatus("ENABLED");

        Conversation conv = new Conversation();
        conv.setId(200L);
        conv.setCustomerId(100L);
        conv.setEmployeeId(10L);
        conv.setStatus(ConversationStatus.AI_ACTIVE);

        var msg = com.ai.cs.gateway.channel.spi.Message.builder()
                .msgId("xhs_msg_1")
                .senderOpenid("xhs_user_123")
                .appId("app_001")
                .content("你好")
                .build();

        when(adapter.normalize(any())).thenReturn(msg);
        when(accountRepo.findByPlatformAndAccountId("XIAOHONGSHU", "app_001"))
                .thenReturn(Optional.of(account));
        when(employeeRepo.findById(10L)).thenReturn(Optional.of(employee));
        when(adapter.syncCustomer("xhs_user_123")).thenReturn(customer);
        when(conversationService.findOrCreateConversation(100L, 10L, "XIAOHONGSHU"))
                .thenReturn(conv);
        when(replyPipelineService.process(200L, 10L, "你好")).thenReturn("回复内容");

        mockMvc.perform(post("/webhook/xiaohongshu/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"msgId\":\"xhs_msg_1\",\"openid\":\"xhs_user_123\",\"appId\":\"app_001\",\"content\":\"你好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    void receiveMessage_shouldReturnOkWhenNoEmployeeConfigured() throws Exception {
        var msg = com.ai.cs.gateway.channel.spi.Message.builder()
                .msgId("xhs_msg_2")
                .senderOpenid("xhs_user_456")
                .appId("unknown_app")
                .content("hello")
                .build();

        when(adapter.normalize(any())).thenReturn(msg);
        when(accountRepo.findByPlatformAndAccountId("XIAOHONGSHU", "unknown_app"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/webhook/xiaohongshu/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"msgId\":\"xhs_msg_2\",\"openid\":\"xhs_user_456\",\"appId\":\"unknown_app\",\"content\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("no employee configured"));
    }

    @Test
    void receiveMessage_shouldReturnOkWhenEmployeeInactive() throws Exception {
        var msg = com.ai.cs.gateway.channel.spi.Message.builder()
                .msgId("xhs_msg_3")
                .senderOpenid("xhs_user_789")
                .appId("app_002")
                .content("test")
                .build();

        AiEmployeeAccount account = new AiEmployeeAccount();
        account.setId(2L);
        account.setEmployeeId(20L);

        AiEmployee employee = new AiEmployee();
        employee.setId(20L);
        employee.setStatus("DISABLED");

        when(adapter.normalize(any())).thenReturn(msg);
        when(accountRepo.findByPlatformAndAccountId("XIAOHONGSHU", "app_002"))
                .thenReturn(Optional.of(account));
        when(employeeRepo.findById(20L)).thenReturn(Optional.of(employee));

        mockMvc.perform(post("/webhook/xiaohongshu/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"msgId\":\"xhs_msg_3\",\"openid\":\"xhs_user_789\",\"appId\":\"app_002\",\"content\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("employee inactive"));
    }

    @Test
    void health_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/webhook/xiaohongshu/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("ok"));
    }
}
