package com.example.novaledger.finance.account.dto;

import com.example.novaledger.finance.account.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAccountRequest {

    @NotNull
    private AccountType accountType;

    @NotBlank
    private String name;

    private String alias;

    @NotBlank
    private String currencyCode;

    @NotNull
    private BigDecimal initialBalance;

    private String bankCode;
    private Long branchId;
    private String accountNumber;
    private String notes;
}
