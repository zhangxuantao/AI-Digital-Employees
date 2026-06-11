package com.ai.cs.domain.assignment.repository;

import com.ai.cs.domain.assignment.HumanAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HumanAgentRepository extends JpaRepository<HumanAgent, Long> {

    List<HumanAgent> findByStatusInAndCurrentLoadLessThan(List<String> statuses, Integer currentLoad);

    Optional<HumanAgent> findByPhone(String phone);
}
