package com.ai.cs.infrastructure.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new CacheService(redisTemplate);
    }

    @Test
    void setShouldStoreValueWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cacheService.set("key1", "value1", Duration.ofMinutes(5));

        verify(valueOps).set("key1", "value1", Duration.ofMinutes(5));
    }

    @Test
    void getShouldReturnStoredValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("existing-key")).thenReturn("stored-value");

        String result = cacheService.get("existing-key");

        assertEquals("stored-value", result);
    }

    @Test
    void getShouldReturnNullForMissingKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("missing-key")).thenReturn(null);

        String result = cacheService.get("missing-key");

        assertNull(result);
    }

    @Test
    void deleteShouldRemoveKey() {
        cacheService.delete("key-to-delete");

        verify(redisTemplate).delete("key-to-delete");
    }

    @Test
    void incrementShouldIncreaseCounter() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("counter")).thenReturn(5L);

        Long result = cacheService.increment("counter");

        assertEquals(5L, result);
        verify(valueOps).increment("counter");
    }

    @Test
    void incrementWithDeltaShouldUseSpecifiedDelta() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("counter", 3L)).thenReturn(8L);

        Long result = cacheService.increment("counter", 3L);

        assertEquals(8L, result);
        verify(valueOps).increment("counter", 3L);
    }
}
