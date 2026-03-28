package com.example.novaledger.finance.bank.repository;

import com.example.novaledger.finance.bank.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankRepository extends JpaRepository<Bank, String> {

    List<Bank> findByIsActiveTrue();

    List<Bank> findByCountry(String country);
}
