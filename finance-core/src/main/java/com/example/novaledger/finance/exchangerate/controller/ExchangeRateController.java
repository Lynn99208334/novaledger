package com.example.novaledger.finance.exchangerate.controller;

import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.finance.exchangerate.dto.ExchangeRateResponse;
import com.example.novaledger.finance.exchangerate.dto.UpdateExchangeRateRequest;
import com.example.novaledger.finance.exchangerate.entity.ExchangeRate;
import com.example.novaledger.finance.exchangerate.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/exchange-rates")
@Tag(name = "Exchange Rates", description = "匯率管理")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping
    @Operation(summary = "取得所有匯率")
    public ResponseEntity<ApiResponse<List<ExchangeRateResponse>>> getAllRates() {
        List<ExchangeRateResponse> responses = exchangeRateService.getAllLatestRates()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PutMapping
    @Operation(summary = "更新匯率（Admin）")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> updateRate(
            @Valid @RequestBody UpdateExchangeRateRequest request) {

        ExchangeRate updated = exchangeRateService.updateRate(
                request.getBaseCurrency(),
                request.getQuoteCurrency(),
                request.getRate()
        );
        return ResponseEntity.ok(ApiResponse.ok(toResponse(updated)));
    }

    private ExchangeRateResponse toResponse(ExchangeRate entity) {
        ExchangeRateResponse res = new ExchangeRateResponse();
        res.setId(entity.getId());
        res.setBaseCurrency(entity.getBaseCurrency());
        res.setQuoteCurrency(entity.getQuoteCurrency());
        res.setRate(entity.getRate());
        res.setRateDate(entity.getRateDate());
        res.setSource(entity.getSource());
        res.setCreatedAt(entity.getCreatedAt());
        return res;
    }
}