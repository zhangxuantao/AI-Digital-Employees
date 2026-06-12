package com.ai.cs.infrastructure.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@ConditionalOnMissingBean(RedisTemplate.class)
public class TestCacheConfig {

    @Bean
    public CacheService cacheService() {
        return new CacheService(null) {
            private final ConcurrentHashMap<String, Object> store = new ConcurrentHashMap<>();
            private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

            @Override
            public void set(String key, Object value, Duration ttl) {
                store.put(key, value);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(String key) {
                return (T) store.get(key);
            }

            @Override
            public void delete(String key) {
                store.remove(key);
            }

            @Override
            public Long increment(String key) {
                return counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
            }

            @Override
            public Long increment(String key, long delta) {
                return counters.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(delta);
            }
        };
    }
}
