package com.example.novaledger.finance.exchangerate.repository;

import com.example.novaledger.finance.exchangerate.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(
            String baseCurrency, String quoteCurrency);

    Optional<ExchangeRate> findByBaseCurrencyAndQuoteCurrencyAndRateDate(
            String baseCurrency, String quoteCurrency, LocalDate rateDate);
}