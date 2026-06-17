package com.example.novaledger.finance.report.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 年報表回應 DTO
 *
 * labels：年份標籤列表，格式 "2025"、"2026"
 * totalAsset：各年度 12 月底總資產（取每年最後一筆快照加總）
 */
@Data
public class YearlyReportResponse {

    private List<String> labels;

    private List<BigDecimal> totalAsset;
}
