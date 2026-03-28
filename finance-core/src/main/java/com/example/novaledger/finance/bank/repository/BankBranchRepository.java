package com.example.novaledger.finance.bank.repository;

import com.example.novaledger.finance.bank.entity.BankBranch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankBranchRepository extends JpaRepository<BankBranch, Long> {

    List<BankBranch> findByBankCode(String bankCode);

    List<BankBranch> findByBankCodeAndIsActiveTrue(String bankCode);
}
