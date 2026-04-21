package com.example.novaledger.finance.transaction.repository;

import com.example.novaledger.finance.transaction.entity.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionItemRepository extends JpaRepository<TransactionItem, Long> {

    List<TransactionItem> findByTenantIdAndTransactionId(Long tenantId, Long transactionId);

    void deleteByTransactionId(Long transactionId);
}