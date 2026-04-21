package com.example.novaledger.finance.account.repository;

import com.example.novaledger.finance.account.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    List<UserAccount> findByTenantIdAndDeletedAtIsNull(Long tenantId);

    List<UserAccount> findByTenantIdAndUserIdAndDeletedAtIsNull(Long tenantId, Long userId);

    Optional<UserAccount> findByIdAndTenantIdAndDeletedAtIsNull(Long id, Long tenantId);

}
