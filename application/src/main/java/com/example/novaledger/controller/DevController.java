package com.example.novaledger.controller;

import com.example.novaledger.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * [DEV ONLY] 提供前端偵測目前執行環境。
 * prod 環境此 endpoint 不存在（404），前端據此隱藏 dev-only 功能。
 */
@Profile("dev")
@RestController
@RequestMapping("/api/dev")
@Tag(name = "Dev", description = "[DEV ONLY] 開發環境工具")
public class DevController {

    @GetMapping("/env")
    @Operation(summary = "[DEV ONLY] 確認目前為 dev 環境")
    public ResponseEntity<ApiResponse<Map<String, String>>> getEnv() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("env", "dev")));
    }
}
