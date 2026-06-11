package com.ai.cs.domain.permission.repository;

import com.ai.cs.domain.permission.SysPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SysPermissionRepository extends JpaRepository<SysPermission, Long> {
}
