package com.ai.cs.domain.employee.repository;

import com.ai.cs.domain.employee.AiEmployeeAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiEmployeeAccountRepository extends JpaRepository<AiEmployeeAccount, Long> {

    Optional<AiEmployeeAccount> findByPlatformAndAccountId(String platform, String accountId);

    List<AiEmployeeAccount> findByEmployeeId(Long employeeId);
}
