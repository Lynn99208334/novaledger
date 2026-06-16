package com.example.novaledger.controller;

import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.dto.AdminUserDto;
import com.example.novaledger.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin - Users", description = "使用者管理（限 ADMIN）")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "分頁查詢使用者（支援關鍵字搜尋）")
    public ResponseEntity<ApiResponse<Page<AdminUserDto>>> getUsers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.getUsers(keyword, page, size)));
    }

    @PatchMapping("/{userId}/disable")
    @Operation(summary = "停用使用者")
    public ResponseEntity<ApiResponse<AdminUserDto>> disableUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.disableUser(userId)));
    }

    @PatchMapping("/{userId}/enable")
    @Operation(summary = "啟用使用者")
    public ResponseEntity<ApiResponse<AdminUserDto>> enableUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.enableUser(userId)));
    }
}
