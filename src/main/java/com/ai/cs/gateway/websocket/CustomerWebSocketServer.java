package com.ai.cs.gateway.websocket;

import com.ai.cs.application.aiemployee.ReplyPipelineService;
import com.ai.cs.application.conversation.ConversationService;
import com.ai.cs.domain.customer.CustomerProfile;
import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.domain.employee.AiEmployeeAccount;
import com.ai.cs.domain.employee.repository.AiEmployeeAccountRepository;
import com.ai.cs.domain.employee.repository.AiEmployeeRepository;
import com.ai.cs.gateway.channel.spi.Message;
import com.ai.cs.gateway.channel.web.WebChannelAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint("/ws/customer/{appId}")
public class CustomerWebSocketServer {

    private static WebChannelAdapter webChannelAdapter;
    private static ConversationService conversationService;
    private static ReplyPipelineService replyPipelineService;
    private static AiEmployeeAccountRepository accountRepo;
    private static AiEmployeeRepository employeeRepo;
    private static ObjectMapper objectMapper;

    // Spring injection via static setters (called from a @PostConstruct or @Configuration)
    public CustomerWebSocketServer(WebChannelAdapter webChannelAdapter,
                                    ConversationService conversationService,
                                    ReplyPipelineService replyPipelineService,
                                    AiEmployeeAccountRepository accountRepo,
                                    AiEmployeeRepository employeeRepo,
                                    ObjectMapper objectMapper) {
        CustomerWebSocketServer.webChannelAdapter = webChannelAdapter;
        CustomerWebSocketServer.conversationService = conversationService;
        CustomerWebSocketServer.replyPipelineService = replyPipelineService;
        CustomerWebSocketServer.accountRepo = accountRepo;
        CustomerWebSocketServer.employeeRepo = employeeRepo;
        CustomerWebSocketServer.objectMapper = objectMapper;
    }

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private String openid;
    private String appId;

    @OnOpen
    public void onOpen(Session session, @PathParam("appId") String appId) {
        this.appId = appId;
        this.openid = session.getId();
        sessions.put(openid, session);
        webChannelAdapter.registerSession(openid, session);
        webChannelAdapter.onCustomerEnter(openid);
        log.info("客户 WebSocket 连接: openid={}, appId={}", openid, appId);
    }

    @OnMessage
    public void onMessage(String text, Session session) {
        log.info("收到客户消息: openid={}, content={}", openid, text);
        try {
            Map<String, Object> raw = Map.of(
                "msgId", UUID.randomUUID().toString(),
                "openid", openid,
                "content", text,
                "appId", appId,
                "msgType", "text"
            );
            Message msg = webChannelAdapter.normalize(raw);

            AiEmployeeAccount account = accountRepo.findByPlatformAndAccountId("WEB", appId).orElse(null);
            if (account == null) {
                sendToClient(session, "系统未配置此渠道的AI员工");
                return;
            }
            AiEmployee employee = employeeRepo.findById(account.getEmployeeId()).orElse(null);
            if (employee == null || !"ENABLED".equals(employee.getStatus())) {
                sendToClient(session, "AI员工不在线");
                return;
            }

            CustomerProfile customer = webChannelAdapter.syncCustomer(openid);
            var conv = conversationService.findOrCreateConversation(customer.getId(), employee.getId(), "WEB");
            replyPipelineService.process(conv.getId(), employee.getId(), msg.getContent());
        } catch (Exception e) {
            log.error("处理客户消息异常", e);
            sendToClient(session, "系统处理异常，请稍后再试");
        }
    }

    @OnClose
    public void onClose() {
        sessions.remove(openid);
        webChannelAdapter.onCustomerLeave(openid);
        log.info("客户 WebSocket 断开: openid={}", openid);
    }

    @OnError
    public void onError(Throwable t) {
        log.error("客户 WebSocket 错误: openid={}", openid, t);
    }

    private void sendToClient(Session session, String content) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("type", "message", "content", content));
            session.getBasicRemote().sendText(json);
        } catch (IOException e) {
            log.error("发送消息到客户失败", e);
        }
    }
}
