package com.example.novaledger.auth.jwt;

import com.example.novaledger.auth.service.RedisBlacklistService;
import com.example.novaledger.common.security.AuthenticatedUserPrincipal;
import com.example.novaledger.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisBlacklistService redisBlacklistService;

    public static final String ACCESS_TOKEN_COOKIE = "access_token";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // ⚑ DA2 擴充點：token 解出 roles 後，未來可改為從動態 RBAC 查詢 permission set
            String token = resolveToken(request);
            if (token != null) {
                if (!jwtTokenProvider.validateToken(token)) {
                    log.warn("action=JWT_VALIDATE result=FAILED reason=INVALID_TOKEN uri={}",
                            request.getRequestURI());
                } else {
                    String jti = jwtTokenProvider.getJti(token);

                    if (redisBlacklistService.isBlacklisted(jti)) {
                        log.warn("Blacklisted JWT detected, jti={}", jti);
                        // Cookie 來源的 blacklisted token：清除 cookie 並導回登入頁
                        if (isFromCookie(request)) {
                            clearAccessTokenCookie(response);
                            response.sendRedirect("/page/login");
                        } else {
                            writeUnauthorizedResponse(response, "JWT token has been logged out");
                        }
                        return;
                    }

                    Long userId = jwtTokenProvider.getUserId(token);
                    String username = jwtTokenProvider.getUsername(token);
                    Long tenantId = jwtTokenProvider.getTenantId(token);
                    List<String> roles = jwtTokenProvider.getRoles(token);

                    AuthenticatedUserPrincipal principal =
                            new AuthenticatedUserPrincipal(userId, username, tenantId, roles, jti);

                    List<GrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);

                    // 새 SecurityContext를 만들어서 명시적으로 set
                    var context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);
                    log.debug("action=JWT_AUTH_SET userId={} roles={} uri={}",
                            userId, roles, request.getRequestURI());
                    TenantContext.setTenantId(tenantId);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 優先從 Authorization header 讀取，其次從 HttpOnly cookie 讀取
     * Header 優先：保留給 Swagger / Postman / 未來 Vue 前端使用
     * Cookie：給 Thymeleaf SSR 頁面使用，讓 sec:authorize 能正確判斷
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. Authorization header（API client / Swagger / Postman）
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        // 2. HttpOnly cookie（Thymeleaf SSR 頁面）
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> ACCESS_TOKEN_COOKIE.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    private boolean isFromCookie(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        return bearer == null || !bearer.startsWith("Bearer ");
    }

    public static void clearAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // dev 環境用 false，prod 改 true
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"success\":false,\"code\":\"JWT_BLACKLISTED\",\"message\":\"%s\"}",
                message
        ));
    }
}
