package com.example.novaledger.common.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class AuthContext {

    public static final String SESSION_USER_ID = "userId";
    public static final String SESSION_CURRENT_TENANT_ID = "CURRENT_TENANT_ID";

    // 未來切換 JWT 時，只改這個 class 的實作
    public Long getCurrentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        Object value = session.getAttribute(SESSION_USER_ID);
        return convertToLong(value);
    }

    public Long getCurrentTenantId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        Object value = session.getAttribute(SESSION_CURRENT_TENANT_ID);
        return convertToLong(value);
    }

    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.valueOf((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }
}