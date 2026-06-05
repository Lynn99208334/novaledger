package com.example.novaledger.integration.auth;

import com.example.novaledger.ratelimit.LoginRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 登入失敗限流整合測試（Redis）
 * 直接測試 LoginRateLimiter，不經過 HTTP 層
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("登入失敗限流整合測試")
class LoginRateLimitIntegrationTest {

    @Autowired
    private LoginRateLimiter loginRateLimiter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // RFC 1918 私有 IP，不會與真實環境衝突
    private static final String TEST_IP = "10.255.255.1";

    @BeforeEach
    void setUp() {
        redisTemplate.delete("login_fail:" + TEST_IP);
        redisTemplate.delete("login_lock:" + TEST_IP);
    }

    @Test
    @DisplayName("初始狀態 → 不應被鎖定")
    void shouldNotBlock_whenNoFailures() {
        assertFalse(loginRateLimiter.isBlocked(TEST_IP));
    }

    @Test
    @DisplayName("只失敗一次 → 不應被鎖定")
    void shouldNotBlock_whenOnlyOneFailure() {
        loginRateLimiter.recordFailure(TEST_IP);
        assertFalse(loginRateLimiter.isBlocked(TEST_IP));
    }

    @Test
    @DisplayName("失敗次數達上限 → 應被鎖定")
    void shouldBlock_whenFailuresReachLimit() {
        // 不依賴 DB 參數值，打到被鎖為止（最多 20 次）
        for (int i = 0; i < 20; i++) {
            loginRateLimiter.recordFailure(TEST_IP);
            if (loginRateLimiter.isBlocked(TEST_IP)) break;
        }
        assertTrue(loginRateLimiter.isBlocked(TEST_IP));
    }

    @Test
    @DisplayName("登入成功後清除失敗計數 → 應解除鎖定")
    void shouldUnblock_afterClearFailures() {
        // 先觸發鎖定
        for (int i = 0; i < 20; i++) {
            loginRateLimiter.recordFailure(TEST_IP);
            if (loginRateLimiter.isBlocked(TEST_IP)) break;
        }
        assertTrue(loginRateLimiter.isBlocked(TEST_IP));

        loginRateLimiter.clearFailures(TEST_IP);

        assertFalse(loginRateLimiter.isBlocked(TEST_IP));
    }
}
