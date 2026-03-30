package com.example.novaledger.finance.account.service;

import com.example.novaledger.common.tenant.TenantContext;
import com.example.novaledger.finance.account.dto.AccountResponse;
import com.example.novaledger.finance.account.dto.CreateAccountRequest;
import com.example.novaledger.finance.account.entity.AccountBalance;
import com.example.novaledger.finance.account.entity.UserAccount;
import com.example.novaledger.finance.account.repository.AccountBalanceRepository;
import com.example.novaledger.finance.account.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {

    private final UserAccountRepository userAccountRepository;
    private final AccountBalanceRepository accountBalanceRepository;

    public AccountService(UserAccountRepository userAccountRepository,
                          AccountBalanceRepository accountBalanceRepository) {
        this.userAccountRepository = userAccountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
    }

    @Transactional
    public AccountResponse createAccount(Long userId, CreateAccountRequest request) {
        Long tenantId = TenantContext.getTenantId();

        UserAccount account = new UserAccount();
        account.setTenantId(tenantId);
        account.setUserId(userId);
        account.setAccountType(request.getAccountType());
        account.setName(request.getName());
        account.setCurrencyCode(request.getCurrencyCode());
        account.setInitialBalance(request.getInitialBalance());
        account.setCurrentBalance(request.getInitialBalance());
        account.setBankCode(request.getBankCode());
        account.setBranchId(request.getBranchId());
        account.setAccountNumber(request.getAccountNumber());
        account.setNotes(request.getNotes());

        UserAccount saved = userAccountRepository.save(account);

        AccountBalance balance = new AccountBalance();
        balance.setTenantId(tenantId);
        balance.setAccountId(saved.getId());
        balance.setSnapshotDate(LocalDate.now());
        balance.setBalance(request.getInitialBalance());
        balance.setCurrencyCode(request.getCurrencyCode());
        accountBalanceRepository.save(balance);

        return AccountResponse.from(saved);
    }

    public List<AccountResponse> getAccounts(Long userId) {
        Long tenantId = TenantContext.getTenantId();
        return userAccountRepository
                .findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, userId)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional
    public AccountResponse updateAccount(Long userId, Long accountId, CreateAccountRequest request) {
        Long tenantId = TenantContext.getTenantId();
        UserAccount account = userAccountRepository
                .findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, userId)
                .stream()
                .filter(a -> a.getId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setName(request.getName());
        account.setCurrencyCode(request.getCurrencyCode());
        account.setBankCode(request.getBankCode());
        account.setBranchId(request.getBranchId());
        account.setAccountNumber(request.getAccountNumber());
        account.setNotes(request.getNotes());

        return AccountResponse.from(userAccountRepository.save(account));
    }

    @Transactional
    public void deleteAccount(Long userId, Long accountId) {
        Long tenantId = TenantContext.getTenantId();
        UserAccount account = userAccountRepository
                .findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, userId)
                .stream()
                .filter(a -> a.getId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setDeletedAt(LocalDateTime.now());
        userAccountRepository.save(account);
    }
}