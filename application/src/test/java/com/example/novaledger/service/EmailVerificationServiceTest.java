package com.example.novaledger.service;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.auth.entity.User;
import com.example.novaledger.auth.repository.UserRepository;
import com.example.novaledger.util.TimeProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Disabled("Service integration test requires database")
class EmailVerificationServiceTest {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private TimeProvider timeProvider;

    /**
     * Test #1
     * token 已過期 → 驗證失敗
     */
    @Test
    void verifyEmail_shouldFail_whenTokenExpired() {

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 10, 0);
        when(timeProvider.now()).thenReturn(now);

        User user = new User();
        user.setUsername("testuser");
        user.setEmail("expired@test.com");
        user.setPassword("password");
        user.setEmailVerified(false);
        user.setEmailVerifyToken("expired-token");
        user.setEmailVerifyExpiredAt(now.minusMinutes(1));

        userRepository.save(user);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> emailVerificationService.verifyEmail("expired-token")
        );

        assertEquals(ErrorCode.EMAIL_VERIFY_TOKEN_EXPIRED, ex.getErrorCode());
    }

    /**
     * Test #2
     * resend 在冷卻時間內 → 應該被擋
     */
    @Test
    void resendVerification_shouldFail_whenInCooldown() {

        LocalDateTime now = LocalDateTime.of(2025, 12, 30, 15, 32);
        when(timeProvider.now()).thenReturn(now);

        User user = new User();
        user.setUsername("cooldown-user");
        user.setEmail("cooldown@test.com");
        user.setPassword("password");
        user.setEmailVerified(false);

        // ⭐ 30 秒前剛寄過信（仍在冷卻時間內）
        user.setEmailVerifyLastSentAt(now.minusSeconds(30));
        user.setEmailVerifyToken("recent-token");
        user.setEmailVerifyExpiredAt(now.plusMinutes(15));

        userRepository.save(user);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> emailVerificationService.resendVerificationEmail("cooldown@test.com")
        );

        assertEquals(ErrorCode.EMAIL_RESEND_TOO_FREQUENT, ex.getErrorCode());
    }

    /**
     * Test #3
     * resend 冷卻已過 → 允許重寄，token 與時間會更新
     */
    @Test
    void resendVerification_shouldUpdateToken_whenCooldownPassed() {

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 10, 0);
        when(timeProvider.now()).thenReturn(now);

        User user = new User();
        user.setUsername("normal-user");
        user.setEmail("normal@test.com");
        user.setPassword("password");
        user.setEmailVerified(false);

        user.setEmailVerifyToken("old-token");
        user.setEmailVerifyExpiredAt(now.minusMinutes(20));

        // ⭐ 冷卻已過（例如 2 分鐘前寄過）
        user.setEmailVerifyLastSentAt(now.minusSeconds(120));

        userRepository.save(user);

        emailVerificationService.resendVerificationEmail("normal@test.com");

        User updated = userRepository.findByEmail("normal@test.com")
                .orElseThrow();

        assertNotNull(updated.getEmailVerifyToken());
        assertNotEquals("old-token", updated.getEmailVerifyToken());
        assertTrue(updated.getEmailVerifyExpiredAt().isAfter(now));
        assertEquals(now, updated.getEmailVerifyLastSentAt());
    }

    /**
     * Test #4（加分測試）
     * 從未寄過驗證信 → 允許 resend
     */
    @Test
    void resendVerification_shouldAllow_whenNeverSentBefore() {

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        when(timeProvider.now()).thenReturn(now);

        User user = new User();
        user.setUsername("first-time-user");
        user.setEmail("first@test.com");
        user.setPassword("password");
        user.setEmailVerified(false);

        // 從未寄過
        user.setEmailVerifyLastSentAt(null);
        user.setEmailVerifyToken(null);
        user.setEmailVerifyExpiredAt(null);

        userRepository.save(user);

        emailVerificationService.resendVerificationEmail("first@test.com");

        User updated = userRepository.findByEmail("first@test.com")
                .orElseThrow();

        assertNotNull(updated.getEmailVerifyToken());
        assertNotNull(updated.getEmailVerifyExpiredAt());
        assertEquals(now, updated.getEmailVerifyLastSentAt());
    }
}
