package com.example.novaledger.service;

import com.example.novaledger.adapter.cache.RedisCacheAdapter;
import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.dto.RegisterRequest;
import com.example.novaledger.auth.entity.User;
import com.example.novaledger.auth.enums.UserStatus;
import com.example.novaledger.auth.repository.UserRepository;
import com.example.novaledger.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class AuthService {

    private final RedisCacheAdapter redisCacheAdapter;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    @Value("${app.auth.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    public AuthService(RedisCacheAdapter redisCacheAdapter,
                       JwtUtil jwtUtil,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder, EmailService emailService) {
        this.redisCacheAdapter = redisCacheAdapter;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // ========================
    // 登出（JWT Blacklist）
    // ========================
    public void logout(String jwt) {
        log.info(">>> AuthService.logout called");

        String jti = jwtUtil.getJti(jwt);
        Date expiration = jwtUtil.getExpiration(jwt);

        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        log.info("Put blacklist jti={}, ttl={}", jti, ttlMillis);

        if (ttlMillis > 0) {
            redisCacheAdapter.putBlacklistJti(jti, ttlMillis);
        }
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


//    public void verifyEmail(String token) {
//
//        User user = userRepository.findByEmailVerifyToken(token)
//                .orElseThrow(() ->
//                        new BusinessException(
//                                ErrorCode.EMAIL_VERIFY_TOKEN_INVALID.getMessage(),
//                                ErrorCode.EMAIL_VERIFY_TOKEN_INVALID,
//                                HttpStatus.BAD_REQUEST
//                        )
//                );
//
//        // 1️⃣ 已驗證過（防重複點）
//        if (Boolean.TRUE.equals(user.getEmailVerified())) {
//            throw new BusinessException(
//                    ErrorCode.EMAIL_ALREADY_VERIFIED.getMessage(),
//                    ErrorCode.EMAIL_ALREADY_VERIFIED,
//                    HttpStatus.BAD_REQUEST
//            );
//        }
//
//        // 2️⃣ token 過期判斷（重點）
//        if (user.getEmailVerifyExpiredAt() == null ||
//                user.getEmailVerifyExpiredAt().isBefore(LocalDateTime.now())) {
//
//            throw new BusinessException(
//                    ErrorCode.EMAIL_VERIFY_TOKEN_EXPIRED.getMessage(),
//                    ErrorCode.EMAIL_VERIFY_TOKEN_EXPIRED,
//                    HttpStatus.BAD_REQUEST
//            );
//        }
//
//        // 3️⃣ 驗證成功 → 更新狀態
//        user.setEmailVerified(true);
//        user.setVerifiedAt(LocalDateTime.now());
//        user.setEmailVerifyToken(null);
//        user.setEmailVerifyExpiredAt(null);
//
//        userRepository.save(user);
//    }
//
//    public void resendVerificationEmail(String email) {
//
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() ->
//                        new BusinessException(
//                                ErrorCode.USER_NOT_FOUND.getMessage(),
//                                ErrorCode.USER_NOT_FOUND,
//                                HttpStatus.BAD_REQUEST
//                        )
//                );
//
//        if (Boolean.TRUE.equals(user.getEmailVerified())) {
//            throw new BusinessException(
//                    ErrorCode.EMAIL_ALREADY_VERIFIED.getMessage(),
//                    ErrorCode.EMAIL_ALREADY_VERIFIED,
//                    HttpStatus.BAD_REQUEST
//            );
//        }
//
//        // 限制重發頻率
//        System.out.println("resendCooldownSeconds=" + resendCooldownSeconds);
//        System.out.println("user.getEmailVerifyExpiredAt()=" + user.getEmailVerifyExpiredAt());
//        if (resendCooldownSeconds > 0 && user.getEmailVerifyExpiredAt() != null) {
//            LocalDateTime lastSendTime =
//                    user.getEmailVerifyExpiredAt().minusMinutes(15); // 原本 token 期限
//
//            System.out.println("lastSendTime=" + lastSendTime);
//            long secondsSinceLastSend =
//                    Duration.between(lastSendTime, LocalDateTime.now()).getSeconds();
//
//            System.out.println("secondsSinceLastSend=" + secondsSinceLastSend);
//            if (secondsSinceLastSend < resendCooldownSeconds) {
//                throw new BusinessException(
//                        ErrorCode.EMAIL_RESEND_TOO_FREQUENT.getMessage(),
//                        ErrorCode.EMAIL_RESEND_TOO_FREQUENT,
//                        HttpStatus.TOO_MANY_REQUESTS
//                );
//            }
//        }
//
//
//        String newToken = generateEmailVerifyToken();
//        LocalDateTime newExpiredAt = generateEmailVerifyExpiredAt();
//
//        user.setEmailVerifyToken(newToken);
//        user.setEmailVerifyExpiredAt(newExpiredAt);
//
//        userRepository.save(user);
//
//        String verifyLink =
//                "http://localhost:8111/api/auth/verify-email?token=" + newToken;
//
//        emailService.sendVerifyEmail(user.getEmail(), verifyLink);
//    }
//
//
    private String generateEmailVerifyToken() {
        return UUID.randomUUID().toString();
    }

    private LocalDateTime generateEmailVerifyExpiredAt() {
        return LocalDateTime.now().plusHours(24);
    }

}
