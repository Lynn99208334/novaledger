package com.example.novaledger.ratelimit;

import com.example.novaledger.common.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 計數版登入限流實作。
 * Key 設計：
 *   login_fail:{ip}   → 失敗次數（Integer，TTL = lockout 分鐘數）
 *   login_lock:{ip}   → 鎖定標記（TTL = lockout 分鐘數，值不重要）
 */
@Slf4j
@Component
public class RedisLoginRateLimiter implements LoginRateLimiter {

    private static final String FAIL_KEY_PREFIX = "login_fail:";
    private static final String LOCK_KEY_PREFIX = "login_lock:";

    private final StringRedisTemplate redisTemplate;
    private final SystemConfigService systemConfigService;

    public RedisLoginRateLimiter(StringRedisTemplate redisTemplate,
                                  SystemConfigService systemConfigService) {
        this.redisTemplate = redisTemplate;
        this.systemConfigService = systemConfigService;
    }

    @Override
    public void recordFailure(String ip) {
        int maxAttempts = systemConfigService.getInteger("auth.login.max.attempts");
        int lockoutMinutes = systemConfigService.getInteger("auth.login.lockout.minutes");

        String failKey = FAIL_KEY_PREFIX + ip;
        String lockKey = LOCK_KEY_PREFIX + ip;

        Long count = redisTemplate.opsForValue().increment(failKey);

        // 第一次失敗時設定 TTL
        if (count != null && count == 1) {
            redisTemplate.expire(failKey, Duration.ofMinutes(lockoutMinutes));
        }

        // 達到上限，設定鎖定標記
        if (count != null && count >= maxAttempts) {
            redisTemplate.opsForValue().set(lockKey, "1", Duration.ofMinutes(lockoutMinutes));
            log.warn("action=LOGIN_BLOCKED ip={} failCount={}", ip, count);
        }
    }

    @Override
    public boolean isBlocked(String ip) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_KEY_PREFIX + ip));
    }

    @Override
    public void clearFailures(String ip) {
        redisTemplate.delete(FAIL_KEY_PREFIX + ip);
        redisTemplate.delete(LOCK_KEY_PREFIX + ip);
    }
}
