package com.example.novaledger.finance.importjob.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ParseResult {

    private boolean success;
    private String errorMessage;

    // 解析成功的欄位
    private LocalDate transactionDate;
    private String description;
    private BigDecimal amount;
    private BigDecimal balance;      // 存摺有，信用卡可為 null
    private int rowNumber;
    private List<String> rawData;


    // 靜態工廠方法
    public static ParseResult success(LocalDate date, String description,
                                      BigDecimal amount, BigDecimal balance,
                                      int rowNumber, List<String> rawData) {
        ParseResult r = new ParseResult();
        r.success = true;
        r.transactionDate = date;
        r.description = description;
        r.amount = amount;
        r.balance = balance;
        r.rowNumber = rowNumber;
        r.rawData = rawData;
        return r;
    }

    public static ParseResult failure(String errorMessage, int rowNumber, List<String> rawData) {
        ParseResult r = new ParseResult();
        r.success = false;
        r.errorMessage = errorMessage;
        r.rowNumber = rowNumber;
        r.rawData = rawData;
        return r;
    }

    // getters

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public List<String> getRawData() {
        return rawData;
    }
}