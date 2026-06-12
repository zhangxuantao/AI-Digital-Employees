package com.ai.cs.domain.assignment.repository;

import com.ai.cs.domain.assignment.AgentChannelPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentChannelPermissionRepository extends JpaRepository<AgentChannelPermission, Long> {
    List<AgentChannelPermission> findByAgentId(Long agentId);
}
