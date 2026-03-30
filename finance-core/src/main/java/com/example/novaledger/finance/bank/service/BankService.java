package com.example.novaledger.finance.bank.service;

import com.example.novaledger.finance.bank.entity.Bank;
import com.example.novaledger.finance.bank.repository.BankRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BankService {

    private final BankRepository bankRepository;

    public BankService(BankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    public List<Bank> getActiveBanks() {
        return bankRepository.findByIsActiveTrue();
    }
}