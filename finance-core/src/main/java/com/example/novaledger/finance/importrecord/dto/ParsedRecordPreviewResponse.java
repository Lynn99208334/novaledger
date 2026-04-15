package com.example.novaledger.finance.importrecord.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ParsedRecordPreviewResponse {
    private Long recordId;
    private Integer rowNumber;
    private LocalDate transactionDate;
    private String description;
    private BigDecimal amount;
    private BigDecimal balance;
    private String currencyCode;
    private String importStatus;

    public ParsedRecordPreviewResponse(Long recordId, Integer rowNumber,
                                       LocalDate transactionDate, String description,
                                       BigDecimal amount, BigDecimal balance,
                                       String currencyCode, String importStatus) {
        this.recordId = recordId;
        this.rowNumber = rowNumber;
        this.transactionDate = transactionDate;
        this.description = description;
        this.amount = amount;
        this.balance = balance;
        this.currencyCode = currencyCode;
        this.importStatus = importStatus;
    }

    public Long getRecordId() { return recordId; }
    public Integer getRowNumber() { return rowNumber; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrencyCode() { return currencyCode; }
    public String getImportStatus() { return importStatus; }
}