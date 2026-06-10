package com.example.novaledger.finance.account.entity;

import com.example.novaledger.common.entity.BaseTenantEntity;
import com.example.novaledger.finance.account.converter.AccountNumberConverter;
import com.example.novaledger.finance.account.enums.AccountType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_accounts")
public class UserAccount extends BaseTenantEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Column(name = "branch_id")
    private Long branchId;

    @Convert(converter = AccountNumberConverter.class)
    @Column(name = "account_number", length = 500)
    private String accountNumber;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "current_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "initial_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal initialBalance;

    @Column(length = 500)
    private String notes;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    /**
     * 回傳遮罩後的帳號，僅顯示後四碼。
     * 例："01312345678" → "****5678"
     * 將在 S20 統一搬移至 SensitiveDataMasker。
     */
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 4) {
            return accountNumber;
        }
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return "****" + last4;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
