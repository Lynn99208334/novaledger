package com.example.novaledger.common.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

@Component
public class TenantInterceptor implements HandlerInterceptor {
    private static final List<String> SYSTEM_ENDPOINTS = List.of(
            "/health",
            "/info"
    );

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/login",
            "/logout",
            "/page/",
            "/api/auth/",
            "/css/",
            "/js/",
            "/images/",
            "/fonts/",
            "/favicon.ico",
            "/error",
            "/swagger-ui",
            "/api/banks",
            "/v3/api-docs"
    );

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        String path = request.getRequestURI();

        if (path.equals("/")) {
            return true;
        }

        if (Stream.concat(
                SYSTEM_ENDPOINTS.stream(),
                PUBLIC_ENDPOINTS.stream()
        ).anyMatch(path::startsWith)) {
            return true;
        }

        if (path.startsWith("/api/")) {
            String tenantIdHeader = request.getHeader(TENANT_HEADER);
            if (tenantIdHeader != null && !tenantIdHeader.isBlank()) {
                TenantContext.setTenantId(Long.valueOf(tenantIdHeader));
                return true;
            }
            HttpSession session = request.getSession(false);
            if (session != null) {
                Long tenantId = (Long) session.getAttribute("tenantId");
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                    return true;
                }
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 頁面路徑：從 session 取 tenantId
        HttpSession session = request.getSession(false);
        if (session != null) {
            Long tenantId = (Long) session.getAttribute("tenantId");
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        TenantContext.clear();
    }
}
