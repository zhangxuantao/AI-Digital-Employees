package com.ai.cs.domain.assignment.repository;

import com.ai.cs.domain.assignment.AgentChannelPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentChannelPermissionRepository extends JpaRepository<AgentChannelPermission, Long> {
}
