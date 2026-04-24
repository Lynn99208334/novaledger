package com.example.novaledger.finance.bank.controller;

import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.finance.bank.dto.BankRequest;
import com.example.novaledger.finance.bank.entity.Bank;
import com.example.novaledger.finance.bank.service.BankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/banks")
@Tag(name = "Admin - Banks", description = "銀行管理（限 ADMIN）")
public class AdminBankController {

    private final BankService bankService;

    public AdminBankController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping
    @Operation(summary = "列出所有銀行（含停用）")
    public ResponseEntity<ApiResponse<List<Bank>>> getBanks() {
        return ResponseEntity.ok(ApiResponse.ok(bankService.getAllBanks()));
    }

    @PostMapping
    @Operation(summary = "新增銀行")
    public ResponseEntity<ApiResponse<Bank>> createBank(
            @Valid @RequestBody BankRequest request) {

        Bank bank = new Bank();
        bank.setBankCode(request.getBankCode());
        bank.setName(request.getName());
        bank.setShortName(request.getShortName());
        bank.setCountry(request.getCountry());
        bank.setIsActive(request.getIsActive());

        Bank created = bankService.createBank(bank);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PutMapping("/{bankCode}")
    @Operation(summary = "更新銀行資料")
    public ResponseEntity<ApiResponse<Bank>> updateBank(
            @PathVariable String bankCode,
            @Valid @RequestBody BankRequest request) {

        Bank updated = new Bank();
        updated.setName(request.getName());
        updated.setShortName(request.getShortName());
        updated.setCountry(request.getCountry());
        updated.setIsActive(request.getIsActive());

        Bank saved = bankService.updateBank(bankCode, updated);
        return ResponseEntity.ok(ApiResponse.ok(saved));
    }
}
