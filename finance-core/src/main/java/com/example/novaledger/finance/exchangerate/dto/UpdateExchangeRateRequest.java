package com.example.novaledger.finance.exchangerate.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class UpdateExchangeRateRequest {

    @NotBlank
    @Size(min = 3, max = 3)
    private String baseCurrency;

    @NotBlank
    @Size(min = 3, max = 3)
    private String quoteCurrency;

    @NotNull
    @DecimalMin(value = "0.000001")
    private BigDecimal rate;

    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }

    public String getQuoteCurrency() { return quoteCurrency; }
    public void setQuoteCurrency(String quoteCurrency) { this.quoteCurrency = quoteCurrency; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
}