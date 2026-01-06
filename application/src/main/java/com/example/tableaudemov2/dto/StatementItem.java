package com.example.tableaudemov2.dto;

import java.math.BigDecimal;
import java.util.List;

public record StatementItem(
        String txnDate,     // 交易日期
        String postDate,    // 入帳日期
        String description, // 交易項目 / 地區
        String currency,    // 幣別
        BigDecimal amount,  // 幣別金額（可能為空）
        BigDecimal twd      // 新臺幣金額
) {}