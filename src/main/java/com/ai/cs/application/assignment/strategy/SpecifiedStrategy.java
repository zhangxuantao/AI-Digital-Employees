package com.ai.cs.application.assignment.strategy;

import com.ai.cs.domain.assignment.HumanAgent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SpecifiedStrategy implements AssignmentStrategy {
    private final ObjectMapper objectMapper;

    @Override public String getType() { return "SPECIFIED"; }

    @Override
    public HumanAgent assign(List<HumanAgent> availableAgents, String configJson, Long employeeId) {
        try {
            JsonNode cfg = objectMapper.readTree(configJson);
            Long targetId = cfg.path("targetAgentId").asLong();
            return availableAgents.stream()
                    .filter(a -> a.getId().equals(targetId))
                    .findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
