package com.example.novaledger.service;

import com.example.novaledger.auth.dto.RegisterRequest;
import com.example.novaledger.auth.entity.User;
import com.example.novaledger.auth.entity.UserTenant;
import com.example.novaledger.auth.enums.UserStatus;
import com.example.novaledger.auth.jwt.JwtTokenProvider;
import com.example.novaledger.auth.repository.UserRepository;
import com.example.novaledger.auth.repository.UserTenantRepository;
import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.common.tenant.AuthContext;
import com.example.novaledger.dto.AuthResponse;
import com.example.novaledger.dto.LoginRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserTenantRepository userTenantRepository;
    private final AuthContext authContext;
    @Value("${app.auth.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder, EmailService emailService, JwtTokenProvider jwtTokenProvider, UserTenantRepository userTenantRepository, AuthContext authContext) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userTenantRepository = userTenantRepository;
        this.authContext = authContext;
    }

    // ========================
    // 註冊
    // ========================
    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        String verifyToken = generateEmailVerifyToken();
        LocalDateTime expiredAt = generateEmailVerifyExpiredAt();

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        System.out.println(">>> password before encode: [" + request.getPassword() + "]");
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setStatus(UserStatus.REGISTERED);
        user.setEmailVerified(false);
        user.setEnabled(true);

        // ⭐ 新增的三行
        user.setEmailVerifyToken(verifyToken);
        user.setEmailVerifyExpiredAt(expiredAt);

        userRepository.save(user);

        // ⭐ 寄送驗證信
        String verifyLink =
                "http://localhost:8111/api/auth/verify-email?token=" + verifyToken;

        emailService.sendVerifyEmail(user.getEmail(), verifyLink);
    }

    public AuthResponse login(LoginRequest request, HttpSession session) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        log.info("input password: {}", request.getPassword());
        log.info("db password: {}", user.getPassword());
        log.info("matches: {}", passwordEncoder.matches(request.getPassword(), user.getPassword()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        Long tenantId = userTenantRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .map(UserTenant::getTenantId)
                .orElse(null);

        log.info("[LOGIN] userId={}, tenantId={}", user.getId(), tenantId);

        session.setAttribute(authContext.SESSION_CURRENT_TENANT_ID, tenantId);

        List<String> roles = List.of("ROLE_USER");

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return new AuthResponse(accessToken, refreshToken);
    }

    private String generateEmailVerifyToken() {
        return UUID.randomUUID().toString();
    }

    private LocalDateTime generateEmailVerifyExpiredAt() {
        return LocalDateTime.now().plusHours(24);
    }

}
