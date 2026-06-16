package com.example.novaledger.controller;

import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.dto.AdminDashboardDto;
import com.example.novaledger.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/page/admin")
@Tag(name = "Admin - Dashboard", description = "Admin 後台（限 ADMIN）")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "admin/dashboard";
    }

    @GetMapping("/audit-logs")
    public String auditLogsPage() {
        return "admin/auditLogList";
    }

    @GetMapping("/parsers")
    public String parsersPage() {
        return "admin/parserOverview";
    }

    @GetMapping("/exchange-rates")
    public String exchangeRatesPage() {
        return "admin/exchangeRates";
    }

    @GetMapping("/system-config")
    public String systemConfigPage() {
        return "admin/systemConfig";
    }

    @GetMapping("/users")
    public String usersPage() {
        return "admin/userList";
    }

    @ResponseBody
    @GetMapping("/api/dashboard/stats")
    @Operation(summary = "取得 Admin Dashboard 系統健康指標")
    public ResponseEntity<ApiResponse<AdminDashboardDto>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.ok(adminDashboardService.getDashboardStats()));
    }
}
