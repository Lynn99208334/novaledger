package com.example.novaledger.common.security;

import java.security.Principal;
import java.util.List;

public class AuthenticatedUserPrincipal implements Principal {

    private final Long userId;
    private final Long tenantId;
    private final List<String> roles;
    private final String jti;
    private final String username;

    public AuthenticatedUserPrincipal(Long userId, Long tenantId, List<String> roles, String jti) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.roles = roles;
        this.jti = jti;
        this.username = null;
    }

    public AuthenticatedUserPrincipal(Long userId, String username, Long tenantId, List<String> roles, String jti) {
        this.userId = userId;
        this.username = username;
        this.tenantId = tenantId;
        this.roles = roles;
        this.jti = jti;
    }

    /**
     * Principal.getName() — Thymeleaf sec:authentication="name" 顯示用
     * username 優先，沒有則 fallback 到 userId
     */
    @Override
    public String getName() {
        return username != null ? username : (userId != null ? userId.toString() : "anonymous");
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getJti() {
        return jti;
    }
}
