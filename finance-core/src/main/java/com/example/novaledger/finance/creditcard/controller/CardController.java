package com.example.novaledger.finance.creditcard.controller;

import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.common.tenant.AuthContext;
import com.example.novaledger.finance.creditcard.dto.CardResponse;
import com.example.novaledger.finance.creditcard.dto.CreateCardRequest;
import com.example.novaledger.finance.creditcard.dto.UpdateCardRequest;
import com.example.novaledger.finance.creditcard.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@Tag(name = "Cards", description = "信用卡管理")
public class CardController {

    private final CardService cardService;
    private final AuthContext authContext;

    public CardController(CardService cardService, AuthContext authContext) {
        this.cardService = cardService;
        this.authContext = authContext;
    }

    @PostMapping
    @Operation(summary = "建立信用卡")
    public ResponseEntity<ApiResponse<CardResponse>> createCard(
            @Valid @RequestBody CreateCardRequest request,
            HttpServletRequest httpRequest) {
        Long userId = authContext.getCurrentUserId();
        CardResponse card = cardService.createCard(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(card));
    }

    @GetMapping
    @Operation(summary = "列出信用卡")
    public ResponseEntity<ApiResponse<List<CardResponse>>> getCards(
            HttpServletRequest httpRequest) {
        Long userId = authContext.getCurrentUserId();
        List<CardResponse> cards = cardService.getCards(userId);
        return ResponseEntity.ok(ApiResponse.ok(cards));
    }

    @PutMapping("/{cardId}")
    @Operation(summary = "更新信用卡")
    public ResponseEntity<ApiResponse<CardResponse>> updateCard(
            @PathVariable Long cardId,
            @Valid @RequestBody UpdateCardRequest request,
            HttpServletRequest httpRequest) {
        Long userId = authContext.getCurrentUserId();
        CardResponse card = cardService.updateCard(userId, cardId, request);
        return ResponseEntity.ok(ApiResponse.ok(card));
    }

    @DeleteMapping("/{cardId}")
    @Operation(summary = "刪除信用卡（軟刪除）")
    public ResponseEntity<ApiResponse<Void>> deleteCard(
            @PathVariable Long cardId,
            HttpServletRequest httpRequest) {
        Long userId = authContext.getCurrentUserId();
        cardService.deleteCard(userId, cardId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}