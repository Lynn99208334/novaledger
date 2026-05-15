package com.example.novaledger.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";
    private static final String BLACKLIST_VALUE = "logout";

    private final StringRedisTemplate stringRedisTemplate;

    public void blacklist(String jti, long remainingSeconds) {
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("JWT jti must not be blank");
        }

        if (remainingSeconds <= 0) {
            return;
        }

        String key = buildKey(jti);
        stringRedisTemplate.opsForValue().set(
                key,
                BLACKLIST_VALUE,
                Duration.ofSeconds(remainingSeconds)
        );
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        try {
            String key = buildKey(jti);
            Boolean exists = stringRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping blacklist check: {}", e.getMessage());
            return false;
        }
    }

    private String buildKey(String jti) {
        return BLACKLIST_KEY_PREFIX + jti;
    }
}