package com.example.novaledger.common.tenant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthContext {

    //未來切換 JWT 時，只改這個 class 的實作
    public Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getSession().getAttribute("userId");
    }
}