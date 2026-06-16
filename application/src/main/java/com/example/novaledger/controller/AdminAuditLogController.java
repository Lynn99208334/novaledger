package com.example.novaledger.controller;

import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.dto.AuditLogDto;
import com.example.novaledger.service.AdminAuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/audit-logs")
@Tag(name = "Admin - Audit Logs", description = "Audit Log 查詢（限 ADMIN）")
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;

    public AdminAuditLogController(AdminAuditLogService adminAuditLogService) {
        this.adminAuditLogService = adminAuditLogService;
    }

    @GetMapping
    @Operation(summary = "分頁查詢 Audit Log（支援 userId / action / 日期區間篩選）")
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> getLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminAuditLogService.getLogs(userId, action, dateFrom, dateTo, page, size)
        ));
    }
}
