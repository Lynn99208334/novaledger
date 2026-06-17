package com.example.novaledger.service;

import com.example.novaledger.auth.dto.RegisterRequest;
import com.example.novaledger.auth.entity.Role;
import com.example.novaledger.auth.entity.Tenant;
import com.example.novaledger.auth.entity.User;
import com.example.novaledger.auth.entity.UserTenant;
import com.example.novaledger.auth.enums.UserStatus;
import com.example.novaledger.auth.enums.UserTenantStatus;
import com.example.novaledger.auth.jwt.JwtTokenProvider;
import com.example.novaledger.auth.repository.RoleRepository;
import com.example.novaledger.auth.repository.TenantRepository;
import com.example.novaledger.auth.repository.UserRepository;
import com.example.novaledger.auth.repository.UserTenantRepository;
import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.common.logging.AuditAction;
import com.example.novaledger.common.logging.AuditLog;
import com.example.novaledger.common.logging.AuditType;
import com.example.novaledger.common.service.SystemConfigService;
import com.example.novaledger.common.tenant.AuthContext;
import com.example.novaledger.common.util.TokenHashUtil;
import com.example.novaledger.dto.AuthResponse;
import com.example.novaledger.dto.LoginRequest;
import com.example.novaledger.dto.RegisterSummary;
import com.example.novaledger.finance.audit.service.AuditLogService;
import com.example.novaledger.ratelimit.LoginRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final AuthContext authContext;
    private final SystemConfigService systemConfigService;
    private final LoginRateLimiter loginRateLimiter;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${app.base-url}")
    private String appBaseUrl;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            JwtTokenProvider jwtTokenProvider,
            UserTenantRepository userTenantRepository,
            TenantRepository tenantRepository,
            RoleRepository roleRepository,
            AuthContext authContext,
            SystemConfigService systemConfigService,
            LoginRateLimiter loginRateLimiter,
            AuditLogService auditLogService,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userTenantRepository = userTenantRepository;
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.authContext = authContext;
        this.systemConfigService = systemConfigService;
        this.loginRateLimiter = loginRateLimiter;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @AuditLog(action = "REGISTER_USER", type = AuditType.CREATE)
    public RegisterSummary register(RegisterRequest request) {
        log.info("action=REGISTER email={}", request.getEmail());

        boolean registrationEnabled = systemConfigService.getBoolean("registration.enabled");
        if (!registrationEnabled) {
            log.warn("action=REGISTER result=FAILED reason=REGISTRATION_DISABLED");
            throw new BusinessException(ErrorCode.REGISTRATION_DISABLED);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("action=REGISTER result=FAILED reason=EMAIL_ALREADY_EXISTS email={}", request.getEmail());
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("action=REGISTER result=FAILED reason=USERNAME_ALREADY_EXISTS username={}", request.getUsername());
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        String rawToken = generateEmailVerifyToken();
        String hashedToken = TokenHashUtil.hashToken(rawToken);
        LocalDateTime expiredAt = generateEmailVerifyExpiredAt();

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.REGISTERED);
        user.setEmailVerified(false);
        user.setEnabled(true);
        user.setEmailVerifyToken(hashedToken);
        user.setEmailVerifyExpiredAt(expiredAt);

        User saved = userRepository.save(user);

        // 建立個人 Tenant
        Tenant tenant = new Tenant();
        tenant.setCode("personal_" + saved.getId());
        tenant.setName(saved.getUsername() + "'s Workspace");
        tenant.setType("PERSONAL");
        tenant.setPlan("FREE");
        tenant.setOwnerUserId(saved.getId());
        tenant.setStatus("ACTIVE");
        Tenant savedTenant = tenantRepository.save(tenant);

        // 查系統預設 MEMBER role
        Role memberRole = roleRepository.findByCodeAndTenantIdIsNull("MEMBER")
                .orElseThrow(() -> {
                    log.error("action=REGISTER result=FAILED reason=MEMBER_ROLE_NOT_FOUND");
                    return new BusinessException(ErrorCode.INTERNAL_ERROR);
                });

        // 建立 UserTenant 關聯
        UserTenant userTenant = new UserTenant();
        userTenant.setUserId(saved.getId());
        userTenant.setTenantId(savedTenant.getId());
        userTenant.setRoleId(memberRole.getId());
        userTenant.setStatus(UserTenantStatus.ACTIVE);
        userTenant.setJoinedAt(LocalDateTime.now());
        userTenantRepository.save(userTenant);

        String verifyLink = appBaseUrl + "/api/auth/verify-email?token=" + rawToken;
        emailService.sendVerifyEmail(saved.getEmail(), verifyLink);

        log.info("action=REGISTER result=SUCCESS userId={} tenantId={}", saved.getId(), savedTenant.getId());
        return new RegisterSummary(saved.getId(), saved.getUsername(), saved.getEmail());
    }

    public AuthResponse login(LoginRequest request, HttpSession session, String ip) {
        log.info("action=LOGIN email={}", request.getEmail());

        if (loginRateLimiter.isBlocked(ip)) {
            log.warn("action=LOGIN result=BLOCKED ip={}", ip);
            throw new BusinessException(ErrorCode.LOGIN_BLOCKED);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    loginRateLimiter.recordFailure(ip);
                    log.warn("action=LOGIN result=FAILED reason=USER_NOT_FOUND email={}", request.getEmail());
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginRateLimiter.recordFailure(ip);
            log.warn("action=LOGIN result=FAILED reason=PASSWORD_INCORRECT userId={}", user.getId());
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            loginRateLimiter.recordFailure(ip);
            log.warn("action=LOGIN result=FAILED reason=EMAIL_NOT_VERIFIED userId={}", user.getId());
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            loginRateLimiter.recordFailure(ip);
            log.warn("action=LOGIN result=FAILED reason=ACCOUNT_DISABLED userId={}", user.getId());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            loginRateLimiter.recordFailure(ip);
            log.warn("action=LOGIN result=FAILED reason=ACCOUNT_NOT_ACTIVE userId={} status={}", user.getId(), user.getStatus());
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        loginRateLimiter.clearFailures(ip);

        Long tenantId = userTenantRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .map(UserTenant::getTenantId)
                .orElse(null);

        session.setAttribute(authContext.SESSION_CURRENT_TENANT_ID, tenantId);

        List<String> roles = Boolean.TRUE.equals(user.getSystemAdmin())
                ? List.of("ROLE_ADMIN")
                : List.of("ROLE_USER");

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), tenantId, roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // 手動寫入 Audit Log：LOGIN 無法透過 AOP 取得正確 userId，在此直接記錄
        writeLoginAuditLog(user, tenantId, ip);

        log.info("action=LOGIN result=SUCCESS userId={} tenantId={}", user.getId(), tenantId);
        return new AuthResponse(accessToken, refreshToken, user.getUsername());
    }

    public void writeLoginAuditLog(User user, Long tenantId, String ip) {
        try {
            String afterValue = objectMapper.writeValueAsString(
                    Map.of("username", user.getUsername(), "roles",
                            Boolean.TRUE.equals(user.getSystemAdmin()) ? "ADMIN" : "MEMBER")
            );
            auditLogService.save(tenantId, user.getId(), AuditAction.LOGIN,
                    "Auth", null, null, afterValue, ip);
        } catch (Exception e) {
            log.error("action=AUDIT_LOGIN_FAILED reason={}", e.getMessage());
        }
    }

    public void writeLogoutAuditLog(String token, String ip) {
        try {
            Long userId = jwtTokenProvider.getUserId(token);
            Long tenantId = jwtTokenProvider.getTenantId(token);
            String username = jwtTokenProvider.getUsername(token);
            String afterValue = objectMapper.writeValueAsString(
                    Map.of("username", username)
            );
            auditLogService.save(tenantId, userId, AuditAction.LOGOUT,
                    "Auth", null, null, afterValue, ip);
        } catch (Exception e) {
            log.error("action=AUDIT_LOGOUT_FAILED reason={}", e.getMessage());
        }
    }

    private String generateEmailVerifyToken() {
        return UUID.randomUUID().toString();
    }

    private LocalDateTime generateEmailVerifyExpiredAt() {
        int expireMinutes = systemConfigService.getInteger("auth.token.expire.minutes");
        return LocalDateTime.now().plusMinutes(expireMinutes);
    }
}
