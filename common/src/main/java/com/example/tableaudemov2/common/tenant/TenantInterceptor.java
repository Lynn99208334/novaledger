package com.example.tableaudemov2.common.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
            "/",
            "/index",
            "/login",
            "/register",
            "/auth/login",
            "/css/",
            "/js/",
            "/images/",
            "/fonts/",
            "/favicon.ico",
            "/error"
    );

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {

        String path = request.getRequestURI();

        // ✅ Step 1：bypass login / health
        if (Stream.concat(
                SYSTEM_ENDPOINTS.stream(),
                PUBLIC_ENDPOINTS.stream()
        ).anyMatch(path::startsWith)) {
            return true;
        }

        // Step 2：強制 tenantId
        String tenantId = request.getHeader(TENANT_HEADER);
        System.out.println(">>> TenantInterceptor HIT");
        System.out.println(">>> URI = " + path);
        System.out.println(">>> X-Tenant-Id = " + tenantId);

        if (tenantId == null || tenantId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // Step 3：設置 TenantContext
        TenantContext.setTenantId(Long.valueOf(tenantId));
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
