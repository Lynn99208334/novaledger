package com.example.novaledger.dto;

import java.util.List;

public record ParseResult(
        String bank,            // 玉山銀行
        String statementMonth,  // 114年09月（擷取自檔頭）
        List<StatementItem> items,
        String rawHeader,       // 檔頭純文字（除錯／比對用）
        String rawTableText     // 表格純文字（除錯／比對用）
) {}
