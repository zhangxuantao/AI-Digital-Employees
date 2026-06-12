package com.ai.cs.infrastructure.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true", matchIfMissing = true)
public class RocketMQConfig {

    public static final String DOC_TRAINING_TOPIC = "doc-training-topic";
    public static final String ASSIGNMENT_NOTIFY_TOPIC = "assignment-notify-topic";

    @Bean
    public RocketMQProperties rocketMQProperties() {
        RocketMQProperties props = new RocketMQProperties();
        log.info("RocketMQ 配置初始化完成");
        return props;
    }
}
