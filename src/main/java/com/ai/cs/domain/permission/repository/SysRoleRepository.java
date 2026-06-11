package com.ai.cs.domain.permission.repository;

import com.ai.cs.domain.permission.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SysRoleRepository extends JpaRepository<SysRole, Long> {
}
