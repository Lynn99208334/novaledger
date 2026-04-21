package com.example.novaledger.finance.transaction.repository;

import com.example.novaledger.finance.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionTypeRepository extends JpaRepository<TransactionType, String> {

    List<TransactionType> findByIsActiveTrue();
}