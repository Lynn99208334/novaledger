package com.example.novaledger.finance.account.entity;

import com.example.novaledger.common.entity.BaseTenantEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "account_balances",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_account_snapshot", columnNames = {"tenant_id", "account_id", "snapshot_date"})
        }
)
public class AccountBalance extends BaseTenantEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
}
