package com.example.novaledger.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Spring Security 用的 UserDetails Adapter
 * 只持有必要欄位，不持有 User entity，確保 session 序列化正確
 */
public class SecurityUser implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final boolean systemAdmin;

    public SecurityUser(Long userId, String email, String password,
                        boolean enabled, boolean systemAdmin) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.systemAdmin = systemAdmin;
    }

    /**
     * 權限清單：systemAdmin = true → ROLE_ADMIN，否則 ROLE_USER
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (systemAdmin) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Long getUserId() {
        return userId;
    }
}
