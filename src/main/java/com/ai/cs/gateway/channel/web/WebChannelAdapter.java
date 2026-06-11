package com.ai.cs.gateway.channel.web;

import com.ai.cs.domain.customer.CustomerProfile;
import com.ai.cs.domain.customer.repository.CustomerProfileRepository;
import com.ai.cs.gateway.channel.spi.ChannelAdapter;
import com.ai.cs.gateway.channel.spi.ChannelType;
import com.ai.cs.gateway.channel.spi.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebChannelAdapter implements ChannelAdapter {

    private final CustomerProfileRepository customerProfileRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, Session> customerSessions = new ConcurrentHashMap<>();

    @Override
    public ChannelType getType() {
        return ChannelType.WEB;
    }

    @Override
    public Message normalize(Object rawMessage) {
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = (Map<String, Object>) rawMessage;
        return Message.builder()
                .msgId((String) raw.get("msgId"))
                .channel(ChannelType.WEB)
                .senderOpenid((String) raw.get("openid"))
                .senderNickname((String) raw.getOrDefault("nickname", "匿名用户"))
                .content((String) raw.get("content"))
                .msgType((String) raw.getOrDefault("msgType", "text"))
                .appId((String) raw.get("appId"))
                .raw(raw)
                .build();
    }

    @Override
    public void send(Long accountId, Message msg) {
        Session session = customerSessions.get(msg.getSenderOpenid());
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(Map.of(
                    "type", "message",
                    "content", msg.getContent(),
                    "msgId", msg.getMsgId()
                ));
                session.getBasicRemote().sendText(json);
            } catch (IOException e) {
                log.error("发送消息到客户失败: openid={}", msg.getSenderOpenid(), e);
                customerSessions.remove(msg.getSenderOpenid());
            }
        }
    }

    @Override
    public CustomerProfile syncCustomer(String openid) {
        return customerProfileRepository.findByPlatformAndOpenid("WEB", openid)
                .orElseGet(() -> {
                    CustomerProfile cp = new CustomerProfile();
                    cp.setPlatform("WEB");
                    cp.setOpenid(openid);
                    cp.setNickname("官网访客");
                    return customerProfileRepository.save(cp);
                });
    }

    @Override
    public void onCustomerEnter(String openid) {
        log.info("Web 客户进入: openid={}", openid);
    }

    @Override
    public void onCustomerLeave(String openid) {
        customerSessions.remove(openid);
        log.info("Web 客户离开: openid={}", openid);
    }

    public void registerSession(String openid, Session session) {
        customerSessions.put(openid, session);
    }
}
