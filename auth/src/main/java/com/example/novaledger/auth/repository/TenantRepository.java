package com.example.novaledger.auth.repository;

import com.example.novaledger.auth.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByIdAndStatus(Long id, String status);

    void deleteByOwnerUserId(Long ownerUserId);
//    Optional<Tenant> findBySlug(String slug);
}

