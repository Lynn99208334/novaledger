package com.example.novaledger.controller;

import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.dto.ParserOverviewDto;
import com.example.novaledger.service.AdminParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/parsers")
@Tag(name = "Admin - Parsers", description = "解析能力總覽（限 ADMIN）")
public class AdminParserController {

    private final AdminParserService adminParserService;

    public AdminParserController(AdminParserService adminParserService) {
        this.adminParserService = adminParserService;
    }

    @GetMapping
    @Operation(summary = "取得所有 parser 的能力總覽")
    public ResponseEntity<ApiResponse<List<ParserOverviewDto>>> getParserOverview() {
        return ResponseEntity.ok(ApiResponse.ok(adminParserService.getParserOverview()));
    }
}
