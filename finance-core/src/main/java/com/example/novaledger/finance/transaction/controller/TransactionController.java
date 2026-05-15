package com.example.novaledger.finance.transaction.controller;

import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.common.tenant.AuthContext;
import com.example.novaledger.finance.transaction.dto.*;
import com.example.novaledger.finance.transaction.entity.Transaction;
import com.example.novaledger.finance.transaction.entity.TransactionItem;
import com.example.novaledger.finance.transaction.repository.TransactionItemRepository;
import com.example.novaledger.finance.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "交易記帳")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionItemRepository transactionItemRepository;
    private final AuthContext authContext;

    public TransactionController(
            TransactionService transactionService,
            TransactionItemRepository transactionItemRepository,
            AuthContext authContext) {
        this.transactionService = transactionService;
        this.transactionItemRepository = transactionItemRepository;
        this.authContext = authContext;
    }

    @PostMapping
    @Operation(summary = "建立交易")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            HttpServletRequest httpRequest) {

        Long userId = authContext.getCurrentUserId();
        Long tenantId = authContext.getCurrentTenantId();

        Transaction tx = toEntity(request, tenantId, userId);
        List<TransactionItem> items = toItemEntities(request.getItems());

        Transaction saved = transactionService.createTransaction(tx, items);
        List<TransactionItem> savedItems = transactionItemRepository
                .findByTenantIdAndTransactionId(tenantId, saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(toResponse(saved, savedItems)));
    }

    @GetMapping
    @Operation(summary = "列出交易（分頁）")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        Long tenantId = authContext.getCurrentTenantId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());

        Page<Transaction> txPage;
        if (accountId != null && from != null && to != null) {
            txPage = transactionService.getTransactionsByAccountAndDateRange(tenantId, accountId, from, to, pageable);
        } else if (accountId != null) {
            txPage = transactionService.getTransactionsByAccount(tenantId, accountId, pageable);
        } else {
            txPage = transactionService.getTransactions(tenantId, pageable);
        }

        Page<TransactionResponse> responsePage = txPage.map(tx -> {
            List<TransactionItem> items = transactionItemRepository
                    .findByTenantIdAndTransactionId(tenantId, tx.getId());
            return toResponse(tx, items);
        });

        return ResponseEntity.ok(ApiResponse.ok(responsePage));
    }

    @PutMapping("/{transactionId}")
    @Operation(summary = "更新交易")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable Long transactionId,
            @Valid @RequestBody UpdateTransactionRequest request,
            HttpServletRequest httpRequest) {

        Long tenantId = authContext.getCurrentTenantId();

        Transaction updated = new Transaction();
        updated.setTxTypeCode(request.getTxTypeCode());
        updated.setTransactionDate(request.getTransactionDate());
        updated.setTotalAmount(request.getTotalAmount());
        updated.setCurrencyCode(request.getCurrencyCode());
        updated.setMemo(request.getMemo());

        List<TransactionItem> newItems = toItemEntities(request.getItems());

        Transaction saved = transactionService.updateTransaction(transactionId, tenantId, updated, newItems);
        List<TransactionItem> savedItems = transactionItemRepository
                .findByTenantIdAndTransactionId(tenantId, saved.getId());

        return ResponseEntity.ok(ApiResponse.ok(toResponse(saved, savedItems)));
    }

    @DeleteMapping("/{transactionId}")
    @Operation(summary = "刪除交易（軟刪除）")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @PathVariable Long transactionId,
            HttpServletRequest httpRequest) {

        Long tenantId = authContext.getCurrentTenantId();
        transactionService.deleteTransaction(transactionId, tenantId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // ─── private helpers ───────────────────────────────────────────

    private Transaction toEntity(CreateTransactionRequest request, Long tenantId, Long userId) {
        Transaction tx = new Transaction();
        tx.setTenantId(tenantId);
        tx.setUserId(userId);
        tx.setAccountId(request.getAccountId());
        tx.setCreditCardId(request.getCreditCardId());
        tx.setTxTypeCode(request.getTxTypeCode());
        tx.setTransactionDate(request.getTransactionDate());
        tx.setTotalAmount(request.getTotalAmount());
        tx.setCurrencyCode(request.getCurrencyCode());
        tx.setMemo(request.getMemo());
        return tx;
    }

    private List<TransactionItem> toItemEntities(List<TransactionItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            return Collections.emptyList();
        }
        return itemRequests.stream().map(req -> {
            TransactionItem item = new TransactionItem();
            item.setAmount(req.getAmount());
            item.setCategoryId(req.getCategoryId());
            item.setMemo(req.getMemo());
            return item;
        }).collect(Collectors.toList());
    }

    private TransactionResponse toResponse(Transaction tx, List<TransactionItem> items) {
        TransactionResponse res = new TransactionResponse();
        res.setId(tx.getId());
        res.setAccountId(tx.getAccountId());
        res.setCreditCardId(tx.getCreditCardId());
        res.setTxTypeCode(tx.getTxTypeCode());
        res.setTransactionDate(tx.getTransactionDate());
        res.setTotalAmount(tx.getTotalAmount());
        res.setCurrencyCode(tx.getCurrencyCode());
        res.setMemo(tx.getMemo());
        res.setCreatedAt(tx.getCreatedAt());
        res.setItems(items.stream().map(item -> {
            TransactionItemResponse itemRes = new TransactionItemResponse();
            itemRes.setId(item.getId());
            itemRes.setCategoryId(item.getCategoryId());
            itemRes.setAmount(item.getAmount());
            itemRes.setMemo(item.getMemo());
            return itemRes;
        }).collect(Collectors.toList()));
        return res;
    }
}