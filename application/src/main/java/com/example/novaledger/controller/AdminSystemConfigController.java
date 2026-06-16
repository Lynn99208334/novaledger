package com.example.novaledger.controller;

import com.example.novaledger.common.entity.SystemConfig;
import com.example.novaledger.common.repository.SystemConfigRepository;
import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.common.service.SystemConfigService;
import com.example.novaledger.common.tenant.AuthContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/system-configs")
@Tag(name = "Admin - System Config", description = "系統設定管理（限 ADMIN）")
public class AdminSystemConfigController {

    private final SystemConfigRepository systemConfigRepository;
    private final SystemConfigService systemConfigService;
    private final AuthContext authContext;

    public AdminSystemConfigController(SystemConfigRepository systemConfigRepository,
                                       SystemConfigService systemConfigService,
                                       AuthContext authContext) {
        this.systemConfigRepository = systemConfigRepository;
        this.systemConfigService = systemConfigService;
        this.authContext = authContext;
    }

    @GetMapping
    @Operation(summary = "取得所有系統設定")
    public ResponseEntity<ApiResponse<List<SystemConfig>>> getAllConfigs() {
        return ResponseEntity.ok(ApiResponse.ok(systemConfigRepository.findAll()));
    }

    @PatchMapping("/{key}")
    @Operation(summary = "更新系統設定值")
    public ResponseEntity<ApiResponse<Void>> updateConfig(
            @PathVariable String key,
            @RequestBody UpdateConfigRequest request) {
        String updatedBy = authContext.getCurrentUserId() != null
                ? authContext.getCurrentUserId().toString()
                : "system";
        systemConfigService.set(key, request.value(), updatedBy);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record UpdateConfigRequest(String value) {}
}
