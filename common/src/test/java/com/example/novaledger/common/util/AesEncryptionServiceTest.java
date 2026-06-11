package com.example.novaledger.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesEncryptionServiceTest {

    // 32 bytes alphanumeric key（測試專用，與任何環境的真實 key 不同）
    private static final String TEST_KEY = "UnitTestOnlyKey00XxYyZzAaBbCcDd1";

    private AesEncryptionService service;

    @BeforeEach
    void setUp() {
        service = new AesEncryptionService(TEST_KEY);
    }

    @Test
    void encrypt_thenDecrypt_shouldReturnOriginalText() {
        String original = "013-12345678-9";
        String encrypted = service.encrypt(original);
        String decrypted = service.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void encrypt_shouldNotEqualPlainText() {
        String original = "013-12345678-9";
        String encrypted = service.encrypt(original);

        assertThat(encrypted).isNotEqualTo(original);
    }

    @Test
    void encrypt_samePlainText_shouldProduceDifferentCipherEachTime() {
        // GCM 每次使用不同 IV，相同明文每次加密結果不同
        String original = "013-12345678-9";
        String encrypted1 = service.encrypt(original);
        String encrypted2 = service.encrypt(original);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    void encrypt_nullInput_shouldReturnNull() {
        assertThat(service.encrypt(null)).isNull();
    }

    @Test
    void encrypt_emptyString_shouldReturnEmpty() {
        assertThat(service.encrypt("")).isEmpty();
    }

    @Test
    void decrypt_nullInput_shouldReturnNull() {
        assertThat(service.decrypt(null)).isNull();
    }

    @Test
    void decrypt_emptyString_shouldReturnEmpty() {
        assertThat(service.decrypt("")).isEmpty();
    }

    @Test
    void constructor_keyNotExactly32Bytes_shouldThrow() {
        assertThatThrownBy(() -> new AesEncryptionService("tooshort"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void decrypt_tamperedCipherText_shouldThrow() {
        // GCM AuthTag 驗證：密文被竄改應拋出例外
        String original = "013-12345678-9";
        String encrypted = service.encrypt(original);

        // 將 Base64 decode 後直接竄改 byte，確保竄改到 AuthTag 區域
        byte[] combined = java.util.Base64.getDecoder().decode(encrypted);
        combined[combined.length - 1] ^= 0xFF; // flip last byte (AuthTag 區域)
        String tampered = java.util.Base64.getEncoder().encodeToString(combined);

        assertThatThrownBy(() -> service.decrypt(tampered))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Decryption failed");
    }
}
