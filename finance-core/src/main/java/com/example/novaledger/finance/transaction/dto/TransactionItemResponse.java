package com.example.novaledger.finance.transaction.dto;

import java.math.BigDecimal;

public class TransactionItemResponse {

    private Long id;
    private Long categoryId;
    private BigDecimal amount;
    private String memo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}