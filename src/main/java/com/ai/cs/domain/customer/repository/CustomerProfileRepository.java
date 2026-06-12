package com.ai.cs.domain.customer.repository;

import com.ai.cs.domain.customer.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    Optional<CustomerProfile> findByPlatformAndOpenid(String platform, String openid);
}
