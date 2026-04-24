package com.example.novaledger.finance.bank.service;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.finance.bank.entity.Bank;
import com.example.novaledger.finance.bank.repository.BankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BankService {

    private final BankRepository bankRepository;

    public BankService(BankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    // ─── 一般使用者 ───────────────────────────────────────────

    public List<Bank> getActiveBanks() {
        return bankRepository.findByIsActiveTrue();
    }

    // ─── Admin ────────────────────────────────────────────────

    public List<Bank> getAllBanks() {
        return bankRepository.findAll();
    }

    @Transactional
    public Bank createBank(Bank bank) {
        if (bankRepository.existsById(bank.getBankCode())) {
            throw new BusinessException(ErrorCode.BANK_CODE_DUPLICATE);
        }
        return bankRepository.save(bank);
    }

    @Transactional
    public Bank updateBank(String bankCode, Bank updated) {
        Bank existing = bankRepository.findById(bankCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_NOT_FOUND));

        existing.setName(updated.getName());
        existing.setShortName(updated.getShortName());
        existing.setCountry(updated.getCountry());
        existing.setIsActive(updated.getIsActive());

        return bankRepository.save(existing);
    }
}
