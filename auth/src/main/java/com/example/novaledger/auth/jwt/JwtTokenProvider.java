package com.example.novaledger.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiration
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;

        log.info("JWT config loaded. accessTokenExpiration={} ms, refreshTokenExpiration={} ms, secretLoaded={}",
                accessTokenExpiration,
                refreshTokenExpiration,
                secret != null && secret.length() >= 32
        );
    }

    public String generateAccessToken(Long userId, Long tenantId, List<String> roles) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(String.valueOf(userId))
                .claim("tenantId", tenantId)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getTenantId(String token) {
        Object tenantId = getClaims(token).get("tenantId");
        if (tenantId == null) return null;
        if (tenantId instanceof Long l) return l;
        if (tenantId instanceof Integer i) return i.longValue();
        return Long.valueOf(tenantId.toString());
    }

    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());  // 加這行
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return getClaims(token).get("roles", List.class);
    }

    public String getJti(String token) {
        return getClaims(token).getId();
    }

    public long getRemainingSeconds(String token) {
        Date expiration = getClaims(token).getExpiration();

        if (expiration == null) {
            return 0;
        }

        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        long remainingSeconds = remainingMillis / 1000;

        return Math.max(remainingSeconds, 0);
    }
}