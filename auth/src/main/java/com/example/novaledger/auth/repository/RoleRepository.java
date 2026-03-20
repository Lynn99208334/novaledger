package com.example.novaledger.auth.repository;

import com.example.novaledger.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    List<Role> findByTenantId(Long tenantId);

    List<Role> findByIsSystemRoleTrue();
}
