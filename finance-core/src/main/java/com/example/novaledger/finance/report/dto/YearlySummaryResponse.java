package com.example.novaledger.finance.report.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class YearlySummaryResponse {

    private String reportYear;

    private Summary summary;

    private List<AccountTypeGroup> typeGroups;

    private List<AccountDetail> accountDetails;

    @Data
    public static class Summary {
        private BigDecimal totalAssetTwd;
        private BigDecimal lastYearTotalAssetTwd;
        private BigDecimal netChangeTwd;
        private BigDecimal netChangePercent;
        private BigDecimal totalIncomeTwd;
        private BigDecimal totalExpenseTwd;
        private BigDecimal netIncomeTwd;
    }

    @Data
    public static class AccountTypeGroup {
        private String type;
        private String displayName;
        private BigDecimal totalTwd;
        private BigDecimal percent;
    }

    @Data
    public static class AccountDetail {
        private Long accountId;
        private String name;
        private String accountType;
        private String accountTypeDisplay;
        private String currencyCode;
        private BigDecimal balanceOriginal;
        private BigDecimal balanceTwd;
        private BigDecimal lastYearBalanceOriginal;
        private BigDecimal lastYearBalanceTwd;
        private BigDecimal changeOriginal;
        private BigDecimal changeTwd;
        private BigDecimal changePercent;
    }
}
