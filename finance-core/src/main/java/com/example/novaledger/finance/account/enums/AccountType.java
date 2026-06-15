package com.example.novaledger.finance.account.enums;

public enum AccountType {
    BANK("銀行帳戶"),
    INVESTMENT("投資帳戶"),
    CASH("現金");

    private final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
