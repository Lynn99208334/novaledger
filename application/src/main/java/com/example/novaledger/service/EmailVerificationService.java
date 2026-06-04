package com.example.novaledger.service;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.auth.entity.User;
import com.example.novaledger.auth.repository.UserRepository;
import com.example.novaledger.common.logging.AuditLog;
import com.example.novaledger.common.logging.AuditType;
import com.example.novaledger.common.service.SystemConfigService;
import com.example.novaledger.dto.VerifySummary;
import com.example.novaledger.util.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final TimeProvider timeProvider;
    private final SystemConfigService systemConfigService;

    @AuditLog(action = "VERIFY_EMAIL", type = AuditType.UPDATE)
    public VerifySummary verifyEmail(String token) {
        User user = userRepository.findByEmailVerifyToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFY_TOKEN_INVALID));

        LocalDateTime now = timeProvider.now();

        if (user.getEmailVerifyExpiredAt() == null || now.isAfter(user.getEmailVerifyExpiredAt())) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFY_TOKEN_EXPIRED);
        }

        user.setEmailVerified(true);
        user.setEmailVerifyToken(null);
        user.setEmailVerifyExpiredAt(null);
        user.setEmailVerifyLastSentAt(null);

        User saved = userRepository.save(user);
        return new VerifySummary(saved.getId(), saved.getEmail(), saved.getEmailVerified());
    }

    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        LocalDateTime now = timeProvider.now();

        if (isInCooldown(user, now)) {
            throw new BusinessException(ErrorCode.EMAIL_RESEND_TOO_FREQUENT);
        }

        int expireMinutes = systemConfigService.getInteger("auth.token.expire.minutes");
        user.setEmailVerifyToken(UUID.randomUUID().toString());
        user.setEmailVerifyExpiredAt(now.plusMinutes(expireMinutes));
        user.setEmailVerifyLastSentAt(now);
        userRepository.save(user);
    }

    boolean isInCooldown(User user, LocalDateTime now) {
        long resendCooldownSeconds = systemConfigService.getInteger("auth.resend.cooldown.seconds");

        if (resendCooldownSeconds <= 0) {
            return false;
        }

        LocalDateTime lastSentAt = user.getEmailVerifyLastSentAt();
        if (lastSentAt == null) {
            return false;
        }

        long secondsSinceLastSend = Duration.between(lastSentAt, now).getSeconds();
        return secondsSinceLastSend < resendCooldownSeconds;
    }
}
