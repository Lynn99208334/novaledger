package com.example.novaledger.controller;

import com.example.novaledger.auth.repository.UserRepository;
import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.common.security.AuthenticatedUserPrincipal;
import com.example.novaledger.dto.tenant.TenantResponse;
import com.example.novaledger.dto.tenant.TenantSwitchRequest;
import com.example.novaledger.service.TenantSwitchService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantSwitchService tenantSwitchService;
    private final UserRepository userRepository;

    public TenantController(TenantSwitchService tenantSwitchService,
                            UserRepository userRepository) {
        this.tenantSwitchService = tenantSwitchService;
        this.userRepository = userRepository;
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TenantResponse>>> findMyTenants() {
        List<TenantResponse> response = tenantSwitchService.findMyTenants();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

//    @GetMapping("/my")
//    public ResponseEntity<ApiResponse<List<TenantResponse>>> findMyTenants(Authentication authentication,
//                                                                           HttpSession session) {
//        Long userId = getCurrentUserId(authentication);
//        List<TenantResponse> response = tenantSwitchService.findMyTenants(userId, session);
//        return ResponseEntity.ok(ApiResponse.ok(response));
//    }

    @PostMapping("/switch")
    public ResponseEntity<ApiResponse<TenantResponse>> switchTenant(@RequestBody TenantSwitchRequest request,
                                                                    Authentication authentication,
                                                                    HttpSession session) {
        Long userId = getCurrentUserId(authentication);
        TenantResponse response = tenantSwitchService.switchTenant(userId, request.getTenantId(), session);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "尚未登入");
        }

        AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) authentication.getPrincipal();
        return principal.getUserId();
    }
}