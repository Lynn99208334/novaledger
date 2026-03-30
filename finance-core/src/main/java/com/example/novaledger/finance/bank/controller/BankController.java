package com.example.novaledger.finance.bank.controller;

import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.finance.bank.entity.Bank;
import com.example.novaledger.finance.bank.service.BankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banks")
@Tag(name = "Banks", description = "銀行查詢")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping
    @Operation(summary = "列出啟用銀行")
    public ResponseEntity<ApiResponse<List<Bank>>> getBanks() {
        List<Bank> banks = bankService.getActiveBanks();
        return ResponseEntity.ok(ApiResponse.ok(banks));
    }
}