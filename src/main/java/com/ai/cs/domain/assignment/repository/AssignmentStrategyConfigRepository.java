package com.ai.cs.domain.assignment.repository;

import com.ai.cs.domain.assignment.AssignmentStrategyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentStrategyConfigRepository extends JpaRepository<AssignmentStrategyConfig, Long> {

    List<AssignmentStrategyConfig> findByEmployeeIdAndIsActive(Long employeeId, Boolean isActive);
}
