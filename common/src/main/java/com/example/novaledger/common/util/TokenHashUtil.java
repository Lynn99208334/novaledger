package com.example.novaledger.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Token 安全雜湊工具。
 *
 * <p>用途：將原始 UUID token hash 後存入 DB，
 * 使 DB 洩漏時攻擊者無法直接使用 token。
 *
 * <p>流程：UUID.randomUUID() → hashToken() → 存 DB
 *         原始 UUID → 放進 email 連結
 *         驗證時：URL 上的 UUID → hashToken() → 比對 DB
 */
public final class TokenHashUtil {

    private TokenHashUtil() {
        // 工具類，禁止實例化
    }

    /**
     * 將原始 token 以 SHA-256 雜湊後回傳 hex string（64 chars）。
     *
     * @param rawToken 原始 token（UUID string）
     * @return SHA-256 hex string
     */
    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 保證提供的演算法，不會發生
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
