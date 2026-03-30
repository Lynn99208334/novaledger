package com.example.novaledger.finance.account.controller;

import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.finance.account.dto.AccountResponse;
import com.example.novaledger.finance.account.dto.CreateAccountRequest;
import com.example.novaledger.finance.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "帳戶管理")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @Operation(summary = "建立帳戶")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @RequestParam Long userId,
            @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse account = accountService.createAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(account));
    }

    @GetMapping
    @Operation(summary = "列出帳戶")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccounts(
            @RequestParam Long userId) {
        List<AccountResponse> accounts = accountService.getAccounts(userId);
        return ResponseEntity.ok(ApiResponse.ok(accounts));
    }

    @PutMapping("/{accountId}")
    @Operation(summary = "更新帳戶")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @PathVariable Long accountId,
            @RequestParam Long userId,
            @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse account = accountService.updateAccount(userId, accountId, request);
        return ResponseEntity.ok(ApiResponse.ok(account));
    }

    @DeleteMapping("/{accountId}")
    @Operation(summary = "刪除帳戶（軟刪除）")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @PathVariable Long accountId,
            @RequestParam Long userId) {
        accountService.deleteAccount(userId, accountId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}