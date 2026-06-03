package com.example.novaledger.finance.exchangerate.service;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.common.logging.AuditContext;
import com.example.novaledger.common.logging.AuditLog;
import com.example.novaledger.common.logging.AuditType;
import com.example.novaledger.finance.exchangerate.entity.ExchangeRate;
import com.example.novaledger.finance.exchangerate.repository.ExchangeRateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final ObjectMapper objectMapper;

    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository,
                                ObjectMapper objectMapper) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.objectMapper = objectMapper;
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

    @AuditLog(action = "UPDATE_EXCHANGE_RATE", type = AuditType.UPDATE)
    @Transactional
    public ExchangeRate updateRate(String baseCurrency, String quoteCurrency, BigDecimal rate) {
        ExchangeRate exchangeRate = exchangeRateRepository
                .findByBaseCurrencyAndQuoteCurrencyAndRateDate(baseCurrency, quoteCurrency, LocalDate.now())
                .orElse(null);

        // 操作前先把舊資料存入 AuditContext（新建時 before = null）
        if (exchangeRate != null) {
            try {
                AuditContext.setBeforeValue(objectMapper.writeValueAsString(exchangeRate));
            } catch (Exception ignored) {}
        }

        if (exchangeRate == null) {
            exchangeRate = new ExchangeRate();
        }

        exchangeRate.setBaseCurrency(baseCurrency);
        exchangeRate.setQuoteCurrency(quoteCurrency);
        exchangeRate.setRate(rate);
        exchangeRate.setRateDate(LocalDate.now());
        exchangeRate.setSource("MANUAL");

        return exchangeRateRepository.save(exchangeRate);
    }
}
