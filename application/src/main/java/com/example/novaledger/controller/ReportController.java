package com.example.novaledger.controller;

import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.common.tenant.AuthContext;
import com.example.novaledger.finance.account.service.AccountSnapshotScheduler;
import com.example.novaledger.finance.report.dto.MonthlyReportResponse;
import com.example.novaledger.finance.report.dto.MonthlySummaryResponse;
import com.example.novaledger.finance.report.dto.YearlyReportResponse;
import com.example.novaledger.finance.report.dto.YearlySummaryResponse;
import com.example.novaledger.finance.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/report")
@Tag(name = "Report", description = "Member 報表 API")
public class ReportController {

    private final ReportService reportService;
    private final AccountSnapshotScheduler accountSnapshotScheduler;
    private final AuthContext authContext;

    public ReportController(ReportService reportService,
                            AccountSnapshotScheduler accountSnapshotScheduler,
                            AuthContext authContext) {
        this.reportService = reportService;
        this.accountSnapshotScheduler = accountSnapshotScheduler;
        this.authContext = authContext;
    }

    /**
     * 月報表 Summary
     * GET /api/report/monthly-summary?month=2026-06
     */
    @GetMapping("/monthly-summary")
    @Operation(summary = "月報表 Summary：總資產、收支、帳戶明細")
    public ResponseEntity<ApiResponse<MonthlySummaryResponse>> getMonthlySummary(
            @RequestParam(required = false) String month) {
        Long tenantId = authContext.requireCurrentTenantId();
        YearMonth ym = (month != null) ? YearMonth.parse(month) : YearMonth.now();
        return ResponseEntity.ok(ApiResponse.ok(reportService.getMonthlySummary(tenantId, ym)));
    }

    /**
     * 年報表 Summary
     * GET /api/report/yearly-summary?year=2026
     */
    @GetMapping("/yearly-summary")
    @Operation(summary = "年報表 Summary：總資產、收支、帳戶明細")
    public ResponseEntity<ApiResponse<YearlySummaryResponse>> getYearlySummary(
            @RequestParam(required = false) Integer year) {
        Long tenantId = authContext.requireCurrentTenantId();
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.ok(reportService.getYearlySummary(tenantId, targetYear)));
    }

    /**
     * 月折線圖（Dashboard 用）
     * GET /api/report/monthly?startMonth=2025-07&endMonth=2026-05
     */
    @GetMapping("/monthly")
    @Operation(summary = "月折線圖資料（Dashboard 用）")
    public ResponseEntity<ApiResponse<MonthlyReportResponse>> getMonthlyReport(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) {
        Long tenantId = authContext.requireCurrentTenantId();
        YearMonth end = (endMonth != null) ? YearMonth.parse(endMonth) : YearMonth.now();
        YearMonth start = (startMonth != null) ? YearMonth.parse(startMonth) : end.minusMonths(11);
        return ResponseEntity.ok(ApiResponse.ok(reportService.getMonthlyReport(tenantId, start, end)));
    }

    /**
     * 年折線圖
     * GET /api/report/yearly?startYear=2025&endYear=2026
     */
    @GetMapping("/yearly")
    @Operation(summary = "年折線圖資料")
    public ResponseEntity<ApiResponse<YearlyReportResponse>> getYearlyReport(
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear) {
        Long tenantId = authContext.requireCurrentTenantId();
        int thisYear = LocalDate.now().getYear();
        int end = (endYear != null) ? endYear : thisYear;
        int start = (startYear != null) ? startYear : thisYear - 1;
        return ResponseEntity.ok(ApiResponse.ok(reportService.getYearlyReport(tenantId, start, end)));
    }

    /**
     * 手動觸發快照（dev 測試用）
     * POST /api/report/snapshot/trigger?date=2026-06-17
     */
    @PostMapping("/snapshot/trigger")
    @Operation(summary = "手動觸發快照（dev 測試用）")
    public ResponseEntity<ApiResponse<String>> triggerSnapshot(
            @RequestParam(required = false) String date) {
        LocalDate snapshotDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        accountSnapshotScheduler.takeSnapshotForDate(snapshotDate);
        return ResponseEntity.ok(ApiResponse.ok("Snapshot triggered for " + snapshotDate));
    }
}
