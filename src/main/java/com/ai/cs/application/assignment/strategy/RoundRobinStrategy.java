package com.ai.cs.application.assignment.strategy;

import com.ai.cs.domain.assignment.HumanAgent;
import com.ai.cs.infrastructure.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoundRobinStrategy implements AssignmentStrategy {
    private final CacheService cacheService;
    private final AtomicLong fallbackCounter = new AtomicLong(0);

    @Override public String getType() { return "ROUND_ROBIN"; }

    @Override
    public HumanAgent assign(List<HumanAgent> availableAgents, String configJson, Long employeeId) {
        if (availableAgents.isEmpty()) return null;
        long counter;
        try {
            String key = "rr:" + employeeId;
            counter = cacheService.increment(key);
            cacheService.set(key + ":ts", System.currentTimeMillis(), Duration.ofHours(24));
        } catch (Exception e) {
            log.warn("Redis不可用，RoundRobin降级为本地计数器: employeeId={}", employeeId);
            counter = fallbackCounter.incrementAndGet();
        }
        int index = (int) (counter % availableAgents.size());
        return availableAgents.get(index);
    }
}
