package com.example.novaledger.controller;

import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * [DEV ONLY] 開發環境專用 Admin API。
 * @Profile("dev") 確保此 Controller 只在 dev 環境載入，prod 不存在這些 endpoint。
 */
@Profile("dev")
@RestController
@RequestMapping("/api/dev/admin/users")
@Tag(name = "Dev Admin - Users", description = "[DEV ONLY] 開發環境使用者管理")
public class DevAdminUserController {

    private final AdminUserService adminUserService;

    public DevAdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "[DEV ONLY] 硬刪除使用者及所有關聯資料")
    public ResponseEntity<ApiResponse<Void>> hardDeleteUser(@PathVariable Long userId) {
        adminUserService.hardDeleteUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
