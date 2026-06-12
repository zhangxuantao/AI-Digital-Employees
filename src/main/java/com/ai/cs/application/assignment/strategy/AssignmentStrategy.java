package com.ai.cs.application.assignment.strategy;

import com.ai.cs.domain.assignment.HumanAgent;
import java.util.List;

public interface AssignmentStrategy {
    String getType();
    HumanAgent assign(List<HumanAgent> availableAgents, String configJson, Long employeeId);
}
