package com.ai.cs.application.assignment.strategy;

import com.ai.cs.domain.assignment.HumanAgent;
import com.ai.cs.infrastructure.cache.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RoundRobinStrategy implements AssignmentStrategy {
    private final CacheService cacheService;

    @Override public String getType() { return "ROUND_ROBIN"; }

    @Override
    public HumanAgent assign(List<HumanAgent> availableAgents, String configJson, Long employeeId) {
        if (availableAgents.isEmpty()) return null;
        String key = "rr:" + employeeId;
        Long counter = cacheService.increment(key);
        cacheService.set(key + ":ts", System.currentTimeMillis(), Duration.ofHours(24));
        int index = (int) (counter % availableAgents.size());
        return availableAgents.get(index);
    }
}
