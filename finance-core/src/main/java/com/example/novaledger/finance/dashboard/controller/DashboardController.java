package com.example.novaledger.finance.dashboard.controller;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.common.tenant.AuthContext;
import com.example.novaledger.finance.dashboard.dto.DashboardSummaryResponse;
import com.example.novaledger.finance.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "總覽儀表板")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthContext authContext;

    public DashboardController(DashboardService dashboardService, AuthContext authContext) {
        this.dashboardService = dashboardService;
        this.authContext = authContext;
    }

    @GetMapping("/summary")
    @Operation(summary = "取得 Dashboard 摘要（總資產、本月收支、分類佔比）")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            HttpServletRequest httpRequest) {

        Long tenantId = authContext.getCurrentTenantId(httpRequest);

        if (tenantId == null) {
            throw new BusinessException(ErrorCode.TENANT_ID_REQUIRED);
        }

        DashboardSummaryResponse summary = dashboardService.getDashboardSummary(tenantId);

        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}