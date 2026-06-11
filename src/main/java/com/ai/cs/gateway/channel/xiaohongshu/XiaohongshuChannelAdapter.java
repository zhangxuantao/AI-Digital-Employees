package com.ai.cs.gateway.channel.xiaohongshu;

import com.ai.cs.domain.customer.CustomerProfile;
import com.ai.cs.domain.customer.repository.CustomerProfileRepository;
import com.ai.cs.gateway.channel.spi.ChannelAdapter;
import com.ai.cs.gateway.channel.spi.ChannelType;
import com.ai.cs.gateway.channel.spi.Message;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class XiaohongshuChannelAdapter implements ChannelAdapter {

    private final CustomerProfileRepository customerProfileRepository;
    private final RateLimiter rateLimiter = RateLimiter.create(5.0); // 5 QPS

    public XiaohongshuChannelAdapter(CustomerProfileRepository customerProfileRepository) {
        this.customerProfileRepository = customerProfileRepository;
    }

    @Override
    public ChannelType getType() {
        return ChannelType.XIAOHONGSHU;
    }

    @Override
    public Message normalize(Object rawMessage) {
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = (Map<String, Object>) rawMessage;
        return Message.builder()
                .msgId((String) raw.get("msgId"))
                .channel(ChannelType.XIAOHONGSHU)
                .senderOpenid((String) raw.get("openid"))
                .senderNickname((String) raw.getOrDefault("nickname", "小红书用户"))
                .content((String) raw.get("content"))
                .msgType((String) raw.getOrDefault("msgType", "text"))
                .appId((String) raw.get("appId"))
                .raw(raw)
                .build();
    }

    @Override
    public void send(Long accountId, Message msg) {
        if (!rateLimiter.tryAcquire()) {
            log.warn("小红书发送限流: accountId={}", accountId);
            return;
        }
        // Phase 2: implement actual Xiaohongshu API call
        log.info("[STUB] 小红书发送消息: accountId={}, content={}", accountId, msg.getContent());
    }

    @Override
    public CustomerProfile syncCustomer(String openid) {
        return customerProfileRepository.findByPlatformAndOpenid("XIAOHONGSHU", openid)
                .orElseGet(() -> {
                    CustomerProfile cp = new CustomerProfile();
                    cp.setPlatform("XIAOHONGSHU");
                    cp.setOpenid(openid);
                    cp.setNickname("小红书用户");
                    return customerProfileRepository.save(cp);
                });
    }

    @Override
    public void onCustomerEnter(String openid) {
        log.info("小红书客户进入: openid={}", openid);
    }

    @Override
    public void onCustomerLeave(String openid) {
        log.info("小红书客户离开: openid={}", openid);
    }
}
