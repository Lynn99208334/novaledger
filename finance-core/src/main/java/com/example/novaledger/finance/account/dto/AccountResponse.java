package com.example.novaledger.finance.account.dto;

import com.example.novaledger.finance.account.entity.UserAccount;
import com.example.novaledger.finance.account.enums.AccountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountResponse {

    private Long id;
    private Long tenantId;
    private Long userId;
    private AccountType accountType;
    private String name;
    private String currencyCode;
    private BigDecimal currentBalance;
    private BigDecimal initialBalance;
    private String bankCode;
    private Long branchId;
    private String accountNumber;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AccountResponse from(UserAccount account) {
        AccountResponse dto = new AccountResponse();
        dto.setId(account.getId());
        dto.setTenantId(account.getTenantId());
        dto.setUserId(account.getUserId());
        dto.setAccountType(account.getAccountType());
        dto.setName(account.getName());
        dto.setCurrencyCode(account.getCurrencyCode());
        dto.setCurrentBalance(account.getCurrentBalance());
        dto.setInitialBalance(account.getInitialBalance());
        dto.setBankCode(account.getBankCode());
        dto.setBranchId(account.getBranchId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setNotes(account.getNotes());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        return dto;
    }
}