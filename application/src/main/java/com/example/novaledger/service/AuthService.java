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
import com.example.novaledger.common.logging.AuditLog;
import com.example.novaledger.common.logging.AuditType;
import com.example.novaledger.common.tenant.AuthContext;
import com.example.novaledger.dto.AuthResponse;
import com.example.novaledger.dto.LoginRequest;
import com.example.novaledger.dto.RegisterSummary;
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
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            JwtTokenProvider jwtTokenProvider,
            UserTenantRepository userTenantRepository,
            AuthContext authContext) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userTenantRepository = userTenantRepository;
        this.authContext = authContext;
    }

    @AuditLog(action = "REGISTER_USER", type = AuditType.CREATE)
    public RegisterSummary register(RegisterRequest request) {
        log.info("action=REGISTER email={}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("action=REGISTER result=FAILED reason=EMAIL_ALREADY_EXISTS email={}", request.getEmail());
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("action=REGISTER result=FAILED reason=USERNAME_ALREADY_EXISTS username={}", request.getUsername());
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        String verifyToken = generateEmailVerifyToken();
        LocalDateTime expiredAt = generateEmailVerifyExpiredAt();

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.REGISTERED);
        user.setEmailVerified(false);
        user.setEnabled(true);
        user.setEmailVerifyToken(verifyToken);
        user.setEmailVerifyExpiredAt(expiredAt);

        User saved = userRepository.save(user);

        String verifyLink = "http://localhost:8111/api/auth/verify-email?token=" + verifyToken;
        emailService.sendVerifyEmail(saved.getEmail(), verifyLink);

        log.info("action=REGISTER result=SUCCESS userId={}", saved.getId());
        return new RegisterSummary(saved.getId(), saved.getUsername(), saved.getEmail());
    }

    public AuthResponse login(LoginRequest request, HttpSession session) {
        log.info("action=LOGIN email={}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("action=LOGIN result=FAILED reason=USER_NOT_FOUND email={}", request.getEmail());
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("action=LOGIN result=FAILED reason=PASSWORD_INCORRECT userId={}", user.getId());
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            log.warn("action=LOGIN result=FAILED reason=EMAIL_NOT_VERIFIED userId={}", user.getId());
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        Long tenantId = userTenantRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .map(UserTenant::getTenantId)
                .orElse(null);

        session.setAttribute(authContext.SESSION_CURRENT_TENANT_ID, tenantId);

        List<String> roles = List.of("ROLE_USER");
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), tenantId, roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        log.info("action=LOGIN result=SUCCESS userId={} tenantId={}", user.getId(), tenantId);
        return new AuthResponse(accessToken, refreshToken, user.getUsername());
    }

    private String generateEmailVerifyToken() {
        return UUID.randomUUID().toString();
    }

    private LocalDateTime generateEmailVerifyExpiredAt() {
        return LocalDateTime.now().plusHours(24);
    }
}
