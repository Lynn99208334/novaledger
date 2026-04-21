package com.example.novaledger.finance.transaction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CreateTransactionRequest {

    private Long accountId;

    private Long creditCardId;

    @NotBlank
    private String txTypeCode;

    @NotNull
    private LocalDate transactionDate;

    @NotNull
    @Positive
    private BigDecimal totalAmount;

    @NotBlank
    private String currencyCode;

    private String memo;

    @Valid
    private List<TransactionItemRequest> items;

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

    public List<TransactionItemRequest> getItems() { return items; }
    public void setItems(List<TransactionItemRequest> items) { this.items = items; }
}