package com.example.novaledger.common.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final AuthContext authContext;

    private static final List<String> SYSTEM_ENDPOINTS = List.of(
            "/health",
            "/info"
    );

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/login",
            "/logout",
            "/page/",
            "/dashboard",
            "/api/auth/",
            "/api/admin/",
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
            Long tenantId = authContext.getCurrentTenantId();

            if (tenantId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"error\":\"Tenant Id Is Required\"}");
                return false;
            }

            TenantContext.setTenantId(tenantId);
            request.setAttribute("tenantId", tenantId);
            return true;
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