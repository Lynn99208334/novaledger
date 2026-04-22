package com.example.novaledger.finance.exchangerate.service;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.finance.exchangerate.entity.ExchangeRate;
import com.example.novaledger.finance.exchangerate.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    public List<ExchangeRate> getAllLatestRates() {
        return exchangeRateRepository.findAll();
    }

    public BigDecimal getLatestRate(String baseCurrency, String quoteCurrency) {
        return exchangeRateRepository
                .findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(baseCurrency, quoteCurrency)
                .map(ExchangeRate::getRate)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND));
    }

    @Transactional
    public ExchangeRate updateRate(String baseCurrency, String quoteCurrency, BigDecimal rate) {
        ExchangeRate exchangeRate = exchangeRateRepository
                .findByBaseCurrencyAndQuoteCurrencyAndRateDate(baseCurrency, quoteCurrency, LocalDate.now())
                .orElse(new ExchangeRate());

        exchangeRate.setBaseCurrency(baseCurrency);
        exchangeRate.setQuoteCurrency(quoteCurrency);
        exchangeRate.setRate(rate);
        exchangeRate.setRateDate(LocalDate.now());
        exchangeRate.setSource("MANUAL");

        return exchangeRateRepository.save(exchangeRate);
    }
}