package com.example.novaledger.finance.account.repository;

import com.example.novaledger.finance.account.entity.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Long> {

    List<AccountBalance> findByTenantIdAndAccountId(Long tenantId, Long accountId);

    Optional<AccountBalance> findByTenantIdAndAccountIdAndSnapshotDate(Long tenantId, Long accountId, LocalDate snapshotDate);
}
