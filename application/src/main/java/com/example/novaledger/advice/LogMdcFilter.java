package com.example.novaledger.advice;

import com.example.novaledger.common.security.AuthenticatedUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 每個 request 進來後，將 userId / tenantId 寫入 MDC。
 * 兩者都從 JWT principal 取，不依賴 session。
 * 搭配 logback-spring.xml 的 %X{userId} / %X{tenantId}，
 * 所有 log 自動帶上這兩個欄位，不需要每行手動加。
 */
@Component
@Order(2)
public class LogMdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            setPrincipalToMdc();
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
            MDC.remove("tenantId");
        }
    }

    private void setPrincipalToMdc() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
                MDC.put("userId", String.valueOf(principal.getUserId()));
                if (principal.getTenantId() != null) {
                    MDC.put("tenantId", String.valueOf(principal.getTenantId()));
                }
            }
        } catch (Exception ignored) {
            // SecurityContext 取不到時靜默略過，不影響主流程
        }
    }
}
