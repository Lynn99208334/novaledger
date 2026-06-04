package com.example.novaledger.security;

import com.example.novaledger.auth.entity.UserTenant;
import com.example.novaledger.auth.repository.UserTenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserTenantRepository userTenantRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        Long userId = securityUser.getUserId();

        Long tenantId = userTenantRepository.findByUserId(userId)
                .stream()
                .findFirst()
                .map(UserTenant::getTenantId)
                .orElse(null);

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        HttpSession session = request.getSession();
        session.setAttribute("tenantId", tenantId);
        session.setAttribute("userId", userId);
        session.setAttribute("roles", roles);

        log.debug("action=LOGIN_SUCCESS userId={} tenantId={} roles={}", userId, tenantId, roles);

        response.sendRedirect("/page/dashboard");
    }
}