package com.ai.cs.gateway.channel.spi;

import com.ai.cs.domain.customer.CustomerProfile;

public interface ChannelAdapter {
    ChannelType getType();
    Message normalize(Object rawMessage);
    void send(Long accountId, Message msg);
    CustomerProfile syncCustomer(String openid);
    void onCustomerEnter(String openid);
    void onCustomerLeave(String openid);
}
