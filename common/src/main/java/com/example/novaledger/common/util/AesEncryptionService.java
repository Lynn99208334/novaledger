package com.example.novaledger.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM column-level 加密服務。
 *
 * <p>存入 DB 的格式（Base64）：IV(12 bytes) + 密文（GCM 模式尾端已包含 AuthTag 16 bytes）
 * <p>Key 來源：環境變數 ACCOUNT_NUMBER_ENCRYPTION_KEY（32 bytes alphanumeric）
 *
 * <p>選用 GCM 而非 CBC 的原因：GCM 自帶 Authentication Tag，
 * 可偵測密文遭竄改；CBC 僅加密無完整性驗證。
 */
@Component
public class AesEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(AesEncryptionService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;    // bytes
    private static final int GCM_TAG_LENGTH = 128;  // bits

    private final SecretKey secretKey;

    public AesEncryptionService(
            @Value("${app.encryption.account-number-key}") String keyString) {
        byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "ACCOUNT_NUMBER_ENCRYPTION_KEY must be exactly 32 bytes (256 bits), got: " + keyBytes.length);
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 加密明文字串。
     *
     * @param plainText 原始明文，null 或空字串直接回傳原值
     * @return Base64 編碼的密文（IV + 密文 + AuthTag）
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 組合：IV(12) + 密文+AuthTag
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * 解密密文字串。
     *
     * @param cipherText Base64 編碼的密文，null 或空字串直接回傳原值
     * @return 原始明文
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            byte[] encrypted = new byte[combined.length - iv.length];
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plainBytes = cipher.doFinal(encrypted);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
