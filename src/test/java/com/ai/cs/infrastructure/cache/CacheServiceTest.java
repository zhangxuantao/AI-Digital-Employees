package com.ai.cs.infrastructure.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CacheServiceTest {

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new CacheService();
        // No Redis injected — tests local in-memory fallback
    }

    @Test
    void setShouldStoreValue() {
        cacheService.set("key1", "value1", Duration.ofMinutes(5));
        assertEquals("value1", cacheService.get("key1"));
    }

    @Test
    void getShouldReturnStoredValue() {
        cacheService.set("existing-key", "stored-value", Duration.ofMinutes(5));
        assertEquals("stored-value", cacheService.get("existing-key"));
    }

    @Test
    void getShouldReturnNullForMissingKey() {
        assertNull(cacheService.get("missing-key"));
    }

    @Test
    void deleteShouldRemoveKey() {
        cacheService.set("key-to-delete", "value", Duration.ofMinutes(5));
        cacheService.delete("key-to-delete");
        assertNull(cacheService.get("key-to-delete"));
    }

    @Test
    void incrementShouldIncreaseCounter() {
        assertEquals(1L, cacheService.increment("counter"));
        assertEquals(2L, cacheService.increment("counter"));
        assertEquals(3L, cacheService.increment("counter"));
    }

    @Test
    void incrementWithDeltaShouldUseSpecifiedDelta() {
        assertEquals(3L, cacheService.increment("counter", 3L));
        assertEquals(8L, cacheService.increment("counter", 5L));
    }

    @Test
    void setAndGetWithDifferentTypes() {
        cacheService.set("int-key", 42, Duration.ofMinutes(5));
        assertEquals(42, (int) cacheService.get("int-key"));

        cacheService.set("bool-key", true, Duration.ofMinutes(5));
        assertEquals(true, cacheService.get("bool-key"));
    }
}
