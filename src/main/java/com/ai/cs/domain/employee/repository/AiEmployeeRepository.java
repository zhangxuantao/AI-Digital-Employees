package com.ai.cs.domain.employee.repository;

import com.ai.cs.domain.employee.AiEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiEmployeeRepository extends JpaRepository<AiEmployee, Long> {

    List<AiEmployee> findByStatus(String status);
}
