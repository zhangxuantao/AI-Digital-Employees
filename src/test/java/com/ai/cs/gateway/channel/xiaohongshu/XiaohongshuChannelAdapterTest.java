package com.ai.cs.gateway.channel.xiaohongshu;

import com.ai.cs.domain.customer.CustomerProfile;
import com.ai.cs.domain.customer.repository.CustomerProfileRepository;
import com.ai.cs.gateway.channel.spi.ChannelType;
import com.ai.cs.gateway.channel.spi.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XiaohongshuChannelAdapterTest {

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    private XiaohongshuChannelAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new XiaohongshuChannelAdapter(customerProfileRepository);
    }

    @Test
    void getType_shouldReturnXiaohongshu() {
        assertEquals(ChannelType.XIAOHONGSHU, adapter.getType());
    }

    @Test
    void normalize_shouldTransformRawMessage() {
        Map<String, Object> raw = Map.of(
                "msgId", "xhs_msg_1",
                "openid", "xhs_user_123",
                "nickname", "小红书用户张三",
                "content", "你好，我想咨询一下",
                "msgType", "text",
                "appId", "app_001"
        );

        Message msg = adapter.normalize(raw);

        assertEquals("xhs_msg_1", msg.getMsgId());
        assertEquals(ChannelType.XIAOHONGSHU, msg.getChannel());
        assertEquals("xhs_user_123", msg.getSenderOpenid());
        assertEquals("小红书用户张三", msg.getSenderNickname());
        assertEquals("你好，我想咨询一下", msg.getContent());
        assertEquals("text", msg.getMsgType());
        assertEquals("app_001", msg.getAppId());
        assertSame(raw, msg.getRaw());
    }

    @Test
    void normalize_shouldUseDefaultNicknameWhenNotProvided() {
        Map<String, Object> raw = Map.of(
                "msgId", "xhs_msg_2",
                "openid", "xhs_user_456",
                "content", "hello"
        );

        Message msg = adapter.normalize(raw);

        assertEquals("小红书用户", msg.getSenderNickname());
        assertEquals("text", msg.getMsgType());
    }

    @Test
    void send_shouldNotThrow() {
        Message msg = Message.builder()
                .msgId("test")
                .channel(ChannelType.XIAOHONGSHU)
                .content("test")
                .build();

        assertDoesNotThrow(() -> adapter.send(1L, msg));
    }

    @Test
    void syncCustomer_shouldReturnExistingCustomer() {
        CustomerProfile existing = new CustomerProfile();
        existing.setId(1L);
        existing.setPlatform("XIAOHONGSHU");
        existing.setOpenid("xhs_user_123");
        existing.setNickname("existing_user");

        when(customerProfileRepository.findByPlatformAndOpenid("XIAOHONGSHU", "xhs_user_123"))
                .thenReturn(Optional.of(existing));

        CustomerProfile result = adapter.syncCustomer("xhs_user_123");

        assertSame(existing, result);
        assertEquals("existing_user", result.getNickname());
    }

    @Test
    void syncCustomer_shouldCreateNewCustomerWhenNotExists() {
        when(customerProfileRepository.findByPlatformAndOpenid("XIAOHONGSHU", "xhs_new_user"))
                .thenReturn(Optional.empty());

        CustomerProfile newCp = new CustomerProfile();
        newCp.setId(2L);
        newCp.setPlatform("XIAOHONGSHU");
        newCp.setOpenid("xhs_new_user");
        newCp.setNickname("小红书用户");

        when(customerProfileRepository.save(any())).thenReturn(newCp);

        CustomerProfile result = adapter.syncCustomer("xhs_new_user");

        assertEquals("XIAOHONGSHU", result.getPlatform());
        assertEquals("xhs_new_user", result.getOpenid());
        assertEquals("小红书用户", result.getNickname());
    }

    @Test
    void onCustomerEnter_shouldLogAndNotThrow() {
        assertDoesNotThrow(() -> adapter.onCustomerEnter("xhs_user_123"));
    }

    @Test
    void onCustomerLeave_shouldLogAndNotThrow() {
        assertDoesNotThrow(() -> adapter.onCustomerLeave("xhs_user_123"));
    }
}
