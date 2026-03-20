package com.example.novaledger.service;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.auth.entity.User;
import com.example.novaledger.auth.repository.UserRepository;
import com.example.novaledger.util.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class EmailVerificationService {


    private final UserRepository userRepository;
    private final TimeProvider timeProvider;
    @Value("${app.auth.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    public EmailVerificationService(UserRepository userRepository, TimeProvider timeProvider) {
        this.userRepository = userRepository;
        this.timeProvider = timeProvider;
    }

    /**
     * Email 驗證
     */
    public void verifyEmail(String token) {

        User user = userRepository.findByEmailVerifyToken(token)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.EMAIL_VERIFY_TOKEN_INVALID)
                );

        LocalDateTime now = timeProvider.now();

        if (user.getEmailVerifyExpiredAt() == null ||
                now.isAfter(user.getEmailVerifyExpiredAt())) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFY_TOKEN_EXPIRED);
        }

        user.setEmailVerified(true);
        user.setEmailVerifyToken(null);
        user.setEmailVerifyExpiredAt(null);
        user.setEmailVerifyLastSentAt(null); // 可選：驗證完成後清掉

        userRepository.save(user);
    }

    /**
     * 重新寄送驗證信
     */
    public void resendVerificationEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        LocalDateTime now = timeProvider.now();

        // ⛔ 冷卻時間判斷（只看 lastSentAt）
        if (isInCooldown(user, now)) {
            throw new BusinessException(ErrorCode.EMAIL_RESEND_TOO_FREQUENT);
        }

        // 產生新 token
        user.setEmailVerifyToken(UUID.randomUUID().toString());
        user.setEmailVerifyExpiredAt(now.plusMinutes(15));

        // ⭐ 關鍵：記錄「這一次寄信的時間」
        user.setEmailVerifyLastSentAt(now);

        userRepository.save(user);

        // send email（略）
    }

    /**
     * 👉 冷卻時間判斷（純 domain 邏輯，可單元測試）
     */
    boolean isInCooldown(User user, LocalDateTime now) {

        if (resendCooldownSeconds <= 0) {
            return false;
        }

        LocalDateTime lastSentAt = user.getEmailVerifyLastSentAt();
        if (lastSentAt == null) {
            return false; // 從未寄過信
        }

        long secondsSinceLastSend =
                Duration.between(lastSentAt, now).getSeconds();

        return secondsSinceLastSend < resendCooldownSeconds;
    }
}
