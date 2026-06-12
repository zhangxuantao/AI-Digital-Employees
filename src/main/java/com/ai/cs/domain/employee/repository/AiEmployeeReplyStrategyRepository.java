package com.ai.cs.domain.employee.repository;

import com.ai.cs.domain.employee.AiEmployeeReplyStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiEmployeeReplyStrategyRepository extends JpaRepository<AiEmployeeReplyStrategy, Long> {

    List<AiEmployeeReplyStrategy> findByEmployeeIdAndEnabledOrderBySortOrderAsc(Long employeeId, Boolean enabled);
}
