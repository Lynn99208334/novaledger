package com.example.novaledger.auth.repository;

import com.example.novaledger.auth.entity.UserTenant;
import com.example.novaledger.auth.enums.UserTenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTenantRepository extends JpaRepository<UserTenant, Long> {

    List<UserTenant> findByUserId(Long userId);

    List<UserTenant> findByTenantId(Long tenantId);

    Optional<UserTenant> findByUserIdAndTenantIdAndStatus(Long userId, Long tenantId, UserTenantStatus status);

    boolean existsByUserIdAndTenantIdAndStatus(Long userId, Long tenantId, UserTenantStatus status);
}
