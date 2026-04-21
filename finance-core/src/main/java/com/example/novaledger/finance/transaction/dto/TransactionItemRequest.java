package com.example.novaledger.finance.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class TransactionItemRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    private Long categoryId;

    private String memo;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}