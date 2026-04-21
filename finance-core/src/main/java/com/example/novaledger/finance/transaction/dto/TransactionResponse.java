package com.example.novaledger.finance.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TransactionResponse {

    private Long id;
    private Long accountId;
    private Long creditCardId;
    private String txTypeCode;
    private LocalDate transactionDate;
    private BigDecimal totalAmount;
    private String currencyCode;
    private String memo;
    private LocalDateTime createdAt;
    private List<TransactionItemResponse> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getCreditCardId() { return creditCardId; }
    public void setCreditCardId(Long creditCardId) { this.creditCardId = creditCardId; }

    public String getTxTypeCode() { return txTypeCode; }
    public void setTxTypeCode(String txTypeCode) { this.txTypeCode = txTypeCode; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<TransactionItemResponse> getItems() { return items; }
    public void setItems(List<TransactionItemResponse> items) { this.items = items; }
}