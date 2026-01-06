package com.example.tableaudemov2.security;

import com.example.tableaudemov2.adapter.cache.RedisCacheAdapter;
import com.example.tableaudemov2.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RedisCacheAdapter redisCacheAdapter;
    private final JwtUtil jwtUtil; // ⭐ 注入 JwtUtil

    public JwtAuthenticationFilter(RedisCacheAdapter redisCacheAdapter,
                                   JwtUtil jwtUtil) {
        this.redisCacheAdapter = redisCacheAdapter;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = resolveToken(request);

        if (jwt != null) {
            // ✅ 使用同一把 key 的 JwtUtil
            String jti = jwtUtil.getJti(jwt);

            if (redisCacheAdapter.isJtiBlacklisted(jti)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // TODO: 正常 JWT 驗證與 SecurityContext 設定
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
