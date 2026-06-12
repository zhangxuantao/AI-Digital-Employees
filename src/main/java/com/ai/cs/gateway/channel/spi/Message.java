package com.ai.cs.gateway.channel.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String msgId;
    private ChannelType channel;
    private String senderOpenid;
    private String senderNickname;
    private String content;
    private String msgType;  // text/image/card
    private LocalDateTime timestamp;
    private Map<String, Object> raw;
    private String appId;
}
