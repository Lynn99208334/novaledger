package com.example.novaledger.finance.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class BankRequest {

    @NotBlank
    @Size(max = 10)
    private String bankCode;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 50)
    private String shortName;

    @NotBlank
    @Size(max = 50)
    private String country;

    @NotNull
    private Boolean isActive;

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
