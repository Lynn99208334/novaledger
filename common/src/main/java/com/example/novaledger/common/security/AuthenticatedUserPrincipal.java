package com.example.novaledger.common.security;

import java.util.List;

public class AuthenticatedUserPrincipal {

    private final Long userId;
    private final List<String> roles;
    private final String jti;

    public AuthenticatedUserPrincipal(Long userId, List<String> roles, String jti) {
        this.userId = userId;
        this.roles = roles;
        this.jti = jti;
    }

    public Long getUserId() {
        return userId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getJti() {
        return jti;
    }
}