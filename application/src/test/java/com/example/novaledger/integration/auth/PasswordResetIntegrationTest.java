package com.example.novaledger.integration.auth;

import com.example.novaledger.auth.entity.User;
import com.example.novaledger.auth.repository.UserRepository;
import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.common.util.TokenHashUtil;
import com.example.novaledger.service.EmailService;
import com.example.novaledger.service.PasswordResetService;
import com.example.novaledger.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 忘記密碼流程整合測試
 * EmailService mock 掉，不實際寄信
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("忘記密碼流程整合測試")
class PasswordResetIntegrationTest {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private TimeProvider timeProvider;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 1, 10, 0);

    @BeforeEach
    void setUp() {
        when(timeProvider.now()).thenReturn(FIXED_NOW);
    }

    @Test
    @DisplayName("有效 token → 重設密碼成功，token 應清除")
    void resetPassword_shouldSucceed_whenTokenValid() {
        // DB 存 hash，模擬真實寫入情境
        String rawToken = "valid-reset-token";
        User user = new User();
        user.setUsername("reset-user");
        user.setEmail("reset@test.com");
        user.setPassword(passwordEncoder.encode("oldPassword"));
        user.setEmailVerified(true);
        user.setPasswordResetToken(TokenHashUtil.hashToken(rawToken));
        user.setPasswordResetExpiredAt(FIXED_NOW.plusMinutes(15));
        userRepository.save(user);

        // service 收到的是原始 token（來自 email 連結）
        passwordResetService.resetPassword(rawToken, "newPassword123");

        User updated = userRepository.findByEmail("reset@test.com").orElseThrow();
        assertTrue(passwordEncoder.matches("newPassword123", updated.getPassword()));
        assertNull(updated.getPasswordResetToken());
        assertNull(updated.getPasswordResetExpiredAt());
    }

    @Test
    @DisplayName("過期 token → 應拋出 PASSWORD_RESET_TOKEN_EXPIRED")
    void resetPassword_shouldFail_whenTokenExpired() {
        // DB 存 hash，token 已過期
        String rawToken = "expired-reset-token";
        User user = new User();
        user.setUsername("expired-reset-user");
        user.setEmail("expired-reset@test.com");
        user.setPassword(passwordEncoder.encode("oldPassword"));
        user.setEmailVerified(true);
        user.setPasswordResetToken(TokenHashUtil.hashToken(rawToken));
        user.setPasswordResetExpiredAt(FIXED_NOW.minusMinutes(1));
        userRepository.save(user);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> passwordResetService.resetPassword(rawToken, "newPassword123")
        );

        assertEquals(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("不存在的 token → 應拋出 PASSWORD_RESET_TOKEN_INVALID")
    void resetPassword_shouldFail_whenTokenNotFound() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> passwordResetService.resetPassword("non-existent-token", "newPassword123")
        );

        assertEquals(ErrorCode.PASSWORD_RESET_TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("申請重設密碼：email 不存在 → 不拋錯（防枚舉攻擊）")
    void requestPasswordReset_shouldNotThrow_whenEmailNotFound() {
        assertDoesNotThrow(() ->
                passwordResetService.requestPasswordReset("nonexistent@test.com")
        );
    }

    @Test
    @DisplayName("申請重設密碼：email 存在 → token 應寫入 DB（以 hash 形式）")
    void requestPasswordReset_shouldSetToken_whenEmailExists() {
        User user = new User();
        user.setUsername("token-user");
        user.setEmail("token@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setEmailVerified(true);
        userRepository.save(user);

        passwordResetService.requestPasswordReset("token@test.com");

        User updated = userRepository.findByEmail("token@test.com").orElseThrow();
        assertNotNull(updated.getPasswordResetToken());
        assertNotNull(updated.getPasswordResetExpiredAt());
        assertTrue(updated.getPasswordResetExpiredAt().isAfter(FIXED_NOW));
        // 確認存的是 hash（64 chars hex），不是原始 UUID
        assertEquals(64, updated.getPasswordResetToken().length());
    }
}
