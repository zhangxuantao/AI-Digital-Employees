package com.ai.cs.application.assignment.strategy;

import com.ai.cs.domain.assignment.HumanAgent;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class LeastLoadStrategy implements AssignmentStrategy {
    @Override public String getType() { return "LEAST_LOAD"; }

    @Override
    public HumanAgent assign(List<HumanAgent> availableAgents, String configJson, Long employeeId) {
        return availableAgents.stream()
                .filter(a -> a.getCurrentLoad() < a.getMaxConcurrent())
                .min(Comparator.comparingInt(HumanAgent::getCurrentLoad))
                .orElse(null);
    }
}
