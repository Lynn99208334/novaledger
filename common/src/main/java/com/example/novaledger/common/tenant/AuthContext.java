package com.example.novaledger.common.tenant;

import com.example.novaledger.common.security.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthContext {

    public static final String SESSION_CURRENT_TENANT_ID = "currentTenantId";

    public Long getCurrentUserId() {
        AuthenticatedUserPrincipal principal = getPrincipal();
        if (principal == null) {
            return null;
        }
        return principal.getUserId();
    }

    public Long getCurrentTenantId() {
        // 優先從 JWT principal 取（前後端分離路徑）
        AuthenticatedUserPrincipal principal = getPrincipal();
        if (principal != null && principal.getTenantId() != null) {
            return principal.getTenantId();
        }

        // fallback：session-based（Thymeleaf / tenant switching）
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpSession session = attrs.getRequest().getSession(false);
        if (session == null) {
            return null;
        }
        Object tenantId = session.getAttribute(SESSION_CURRENT_TENANT_ID);
        if (tenantId instanceof Long) {
            return (Long) tenantId;
        }
        if (tenantId instanceof Integer) {
            return ((Integer) tenantId).longValue();
        }
        return null;
    }

    public Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User Id Is Required");
        }
        return userId;
    }

    public Long requireCurrentTenantId() {
        Long tenantId = getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant Id Is Required");
        }
        return tenantId;
    }

    public String getCurrentJti() {
        AuthenticatedUserPrincipal principal = getPrincipal();
        if (principal == null) {
            return null;
        }
        return principal.getJti();
    }

    private AuthenticatedUserPrincipal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthenticatedUserPrincipal)) {
            return null;
        }
        return (AuthenticatedUserPrincipal) principal;
    }
}