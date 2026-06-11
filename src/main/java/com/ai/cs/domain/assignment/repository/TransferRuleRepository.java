package com.ai.cs.domain.assignment.repository;

import com.ai.cs.domain.assignment.TransferRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRuleRepository extends JpaRepository<TransferRule, Long> {

    List<TransferRule> findByEmployeeIdAndEnabledOrderByPriorityAsc(Long employeeId, Boolean enabled);
}
