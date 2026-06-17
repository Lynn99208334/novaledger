package com.example.novaledger.integration.auth;

import com.example.novaledger.auth.entity.User;
import com.example.novaledger.auth.repository.UserRepository;
import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.common.util.TokenHashUtil;
import com.example.novaledger.service.EmailService;
import com.example.novaledger.service.EmailVerificationService;
import com.example.novaledger.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Email 驗證流程整合測試
 * EmailService mock 掉，不實際寄信
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Email 驗證流程整合測試")
class EmailVerificationIntegrationTest {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private UserRepository userRepository;

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
    @DisplayName("有效 token → 驗證成功，emailVerified 應為 true")
    void verifyEmail_shouldSucceed_whenTokenValid() {
        // DB 存 hash，模擬真實寫入情境
        String rawToken = "valid-token";
        User user = new User();
        user.setUsername("verify-user");
        user.setEmail("verify@test.com");
        user.setPassword("password");
        user.setEmailVerified(false);
        user.setEmailVerifyToken(TokenHashUtil.hashToken(rawToken));
        user.setEmailVerifyExpiredAt(FIXED_NOW.plusMinutes(15));
        userRepository.save(user);

        // service 收到的是原始 token（來自 email 連結）
        emailVerificationService.verifyEmail(rawToken);

        User updated = userRepository.findByEmail("verify@test.com").orElseThrow();
        assertTrue(updated.getEmailVerified());
        assertNull(updated.getEmailVerifyToken());
    }

    @Test
    @DisplayName("過期 token → 應拋出 EMAIL_VERIFY_TOKEN_EXPIRED")
    void verifyEmail_shouldFail_whenTokenExpired() {
        // DB 存 hash，token 已過期
        String rawToken = "expired-token";
        User user = new User();
        user.setUsername("expired-user");
        user.setEmail("expired@test.com");
        user.setPassword("password");
        user.setEmailVerified(false);
        user.setEmailVerifyToken(TokenHashUtil.hashToken(rawToken));
        user.setEmailVerifyExpiredAt(FIXED_NOW.minusMinutes(1));
        userRepository.save(user);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> emailVerificationService.verifyEmail(rawToken)
        );

        assertEquals(ErrorCode.EMAIL_VERIFY_TOKEN_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("不存在的 token → 應拋出 EMAIL_VERIFY_TOKEN_INVALID")
    void verifyEmail_shouldFail_whenTokenNotFound() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> emailVerificationService.verifyEmail("non-existent-token")
        );

        assertEquals(ErrorCode.EMAIL_VERIFY_TOKEN_INVALID, ex.getErrorCode());
    }
}
