package com.example.novaledger.service;

import com.example.novaledger.auth.entity.User;
import com.example.novaledger.auth.repository.UserRepository;
import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.common.service.SystemConfigService;
import com.example.novaledger.common.util.TokenHashUtil;
import com.example.novaledger.util.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class PasswordResetService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider timeProvider;
    private final SystemConfigService systemConfigService;

    @Value("${app.base-url}")
    private String baseUrl;

    public PasswordResetService(UserRepository userRepository,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder,
                                TimeProvider timeProvider,
                                SystemConfigService systemConfigService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.timeProvider = timeProvider;
        this.systemConfigService = systemConfigService;
    }

    /**
     * 申請忘記密碼：產生 token，寄重設信。
     * 找不到 email 時不拋錯（避免 email 枚舉攻擊）。
     * DB 存 SHA-256 hash，email 連結帶原始 UUID。
     */
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            int expireMinutes = systemConfigService.getInteger("auth.token.expire.minutes");
            LocalDateTime now = timeProvider.now();

            String rawToken = UUID.randomUUID().toString();
            String hashedToken = TokenHashUtil.hashToken(rawToken);

            user.setPasswordResetToken(hashedToken);
            user.setPasswordResetExpiredAt(now.plusMinutes(expireMinutes));
            userRepository.save(user);

            String resetLink = baseUrl + "/page/reset-password?token=" + rawToken;
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
            log.info("Password reset token issued for userId={}", user.getId());
        });
    }

    /**
     * 重設密碼：驗證 token，更新密碼，清除 token。
     * 從 URL 取得原始 token → hash → 比對 DB。
     */
    public void resetPassword(String token, String newPassword) {
        String hashedToken = TokenHashUtil.hashToken(token);

        User user = userRepository.findByPasswordResetToken(hashedToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        LocalDateTime now = timeProvider.now();
        if (user.getPasswordResetExpiredAt() == null || now.isAfter(user.getPasswordResetExpiredAt())) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiredAt(null);
        userRepository.save(user);

        log.info("Password reset completed for userId={}", user.getId());
    }
}
