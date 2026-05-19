package com.example.novaledger.common.security;

import java.util.List;

public class AuthenticatedUserPrincipal {

    private final Long userId;
    private final Long tenantId;
    private final List<String> roles;
    private final String jti;

    public AuthenticatedUserPrincipal(Long userId, Long tenantId, List<String> roles, String jti) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.roles = roles;
        this.jti = jti;
    }

    public Long getUserId() {
        return userId;
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
