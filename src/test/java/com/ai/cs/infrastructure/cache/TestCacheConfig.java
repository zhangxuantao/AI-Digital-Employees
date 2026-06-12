package com.ai.cs.infrastructure.cache;

import org.springframework.context.annotation.Configuration;

/**
 * CacheService now has built-in in-memory fallback when RedisTemplate is not available.
 * No custom test configuration needed.
 */
@Configuration
public class TestCacheConfig {
}
