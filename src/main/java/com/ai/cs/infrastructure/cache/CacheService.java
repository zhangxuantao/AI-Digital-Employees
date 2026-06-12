package com.ai.cs.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class CacheService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // In-memory fallback when Redis is not available
    private final Map<String, Object> localCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> localCounters = new ConcurrentHashMap<>();

    private boolean isRedisAvailable() {
        return redisTemplate != null;
    }

    public void set(String key, Object value, Duration ttl) {
        if (isRedisAvailable()) {
            redisTemplate.opsForValue().set(key, value, ttl);
        } else {
            localCache.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (isRedisAvailable()) {
            return (T) redisTemplate.opsForValue().get(key);
        }
        return (T) localCache.get(key);
    }

    public void delete(String key) {
        if (isRedisAvailable()) {
            redisTemplate.delete(key);
        } else {
            localCache.remove(key);
        }
    }

    public Long increment(String key) {
        return increment(key, 1);
    }

    public Long increment(String key, long delta) {
        if (isRedisAvailable()) {
            return redisTemplate.opsForValue().increment(key, delta);
        }
        return localCounters.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(delta);
    }
}
