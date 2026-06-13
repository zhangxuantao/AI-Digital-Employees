package com.ai.cs.infrastructure.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置类 — 定义 Topic 常量。
 * RocketMQProperties Bean 由 rocketmq-spring-boot-starter 自动配置提供。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true", matchIfMissing = true)
public class RocketMQConfig {

    public static final String DOC_TRAINING_TOPIC = "doc-training-topic";
    public static final String ASSIGNMENT_NOTIFY_TOPIC = "assignment-notify-topic";
}
