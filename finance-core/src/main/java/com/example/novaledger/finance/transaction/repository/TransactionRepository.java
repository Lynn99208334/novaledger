package com.example.novaledger.finance.transaction.repository;

import com.example.novaledger.finance.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByTenantIdAndDeletedAtIsNull(Long tenantId, Pageable pageable);

    Page<Transaction> findByTenantIdAndAccountIdAndDeletedAtIsNull(Long tenantId, Long accountId, Pageable pageable);

    Page<Transaction> findByTenantIdAndAccountIdAndTransactionDateBetweenAndDeletedAtIsNull(
            Long tenantId, Long accountId, LocalDate from, LocalDate to, Pageable pageable);

    Optional<Transaction> findByIdAndTenantIdAndDeletedAtIsNull(Long id, Long tenantId);
}