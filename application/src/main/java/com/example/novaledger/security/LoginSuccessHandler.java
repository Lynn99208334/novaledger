package com.example.novaledger.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // formLogin 目前未使用（前端透過 /api/auth/login JWT 登入）
        // role-based redirect 邏輯在 login.js 處理
        // ⚑ DA2 擴充點：若未來改回 formLogin，在此加入 role-based redirect 邏輯
        log.debug("action=FORM_LOGIN_SUCCESS user={}", authentication.getName());
        response.sendRedirect("/page/dashboard");
    }
}
