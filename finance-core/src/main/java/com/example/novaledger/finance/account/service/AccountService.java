package com.example.novaledger.finance.account.service;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.common.logging.AuditContext;
import com.example.novaledger.common.logging.AuditLog;
import com.example.novaledger.common.logging.AuditType;
import com.example.novaledger.common.tenant.AuthContext;
import com.example.novaledger.finance.account.dto.AccountResponse;
import com.example.novaledger.finance.account.dto.CreateAccountRequest;
import com.example.novaledger.finance.account.entity.AccountBalance;
import com.example.novaledger.finance.account.entity.UserAccount;
import com.example.novaledger.finance.account.repository.AccountBalanceRepository;
import com.example.novaledger.finance.account.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {

    private final UserAccountRepository userAccountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AuthContext authContext;
    private final ObjectMapper objectMapper;

    public AccountService(UserAccountRepository userAccountRepository,
                          AccountBalanceRepository accountBalanceRepository,
                          AuthContext authContext,
                          ObjectMapper objectMapper) {
        this.userAccountRepository = userAccountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.authContext = authContext;
        this.objectMapper = objectMapper;
    }

    @AuditLog(action = "CREATE_ACCOUNT", type = AuditType.CREATE)
    @Transactional
    public AccountResponse createAccount(Long userId, CreateAccountRequest request) {
        Long tenantId = authContext.getCurrentTenantId();

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
        Long tenantId = authContext.getCurrentTenantId();
        return userAccountRepository
                .findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, userId)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    @AuditLog(action = "UPDATE_ACCOUNT", type = AuditType.UPDATE)
    @Transactional
    public AccountResponse updateAccount(Long userId, Long accountId, CreateAccountRequest request) {
        Long tenantId = authContext.getCurrentTenantId();
        UserAccount account = userAccountRepository
                .findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, userId)
                .stream()
                .filter(a -> a.getId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_001));

        // 操作前先把舊資料存入 AuditContext
        try {
            AuditContext.setBeforeValue(objectMapper.writeValueAsString(AccountResponse.from(account)));
        } catch (Exception ignored) {}

        account.setName(request.getName());
        account.setCurrencyCode(request.getCurrencyCode());
        account.setBankCode(request.getBankCode());
        account.setBranchId(request.getBranchId());
        account.setAccountNumber(request.getAccountNumber());
        account.setNotes(request.getNotes());

        return AccountResponse.from(userAccountRepository.save(account));
    }

    @AuditLog(action = "DELETE_ACCOUNT", type = AuditType.DELETE)
    @Transactional
    public void deleteAccount(Long userId, Long accountId) {
        Long tenantId = authContext.getCurrentTenantId();
        UserAccount account = userAccountRepository
                .findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, userId)
                .stream()
                .filter(a -> a.getId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // 操作前先把舊資料存入 AuditContext
        try {
            AuditContext.setBeforeValue(objectMapper.writeValueAsString(AccountResponse.from(account)));
        } catch (Exception ignored) {}

        account.setDeletedAt(LocalDateTime.now());
        userAccountRepository.save(account);
    }

    @Transactional
    public void toggleActive(Long userId, Long accountId) {
        Long tenantId = authContext.getCurrentTenantId();
        UserAccount account = userAccountRepository
                .findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, userId)
                .stream()
                .filter(a -> a.getId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_001));
        userAccountRepository.save(account);
    }
}
