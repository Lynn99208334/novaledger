package com.example.novaledger.finance.report.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 月報表回應 DTO
 *
 * labels：月份標籤列表，格式 "2025-07" ～ "2026-05"
 * accounts：各帳戶每月餘額折線資料，key = 帳戶名稱（含幣別），value = 對應每月餘額列表
 * totalAsset：總資產每月加總（TWD 基準，USD 帳戶不做換算，直接加總供折線參考）
 */
@Data
public class MonthlyReportResponse {

    private List<String> labels;

    private Map<String, List<BigDecimal>> accounts;

    private List<BigDecimal> totalAsset;
}
