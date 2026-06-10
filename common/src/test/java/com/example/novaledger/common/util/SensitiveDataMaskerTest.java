package com.example.novaledger.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    // ── maskAccountNumber ────────────────────────────────────

    @Nested
    @DisplayName("maskAccountNumber")
    class MaskAccountNumber {

        @Test
        @DisplayName("null 回傳 null")
        void null_returnsNull() {
            assertThat(SensitiveDataMasker.maskAccountNumber(null)).isNull();
        }

        @Test
        @DisplayName("長度不足 4 回傳原值")
        void shortString_returnsOriginal() {
            assertThat(SensitiveDataMasker.maskAccountNumber("123")).isEqualTo("123");
            assertThat(SensitiveDataMasker.maskAccountNumber("")).isEqualTo("");
        }

        @Test
        @DisplayName("長度剛好 4 回傳 ****XXXX")
        void exactly4_returnsMasked() {
            assertThat(SensitiveDataMasker.maskAccountNumber("5678")).isEqualTo("****5678");
        }

        @Test
        @DisplayName("正常長度帳號回傳後四碼遮罩")
        void normalLength_returnsMasked() {
            assertThat(SensitiveDataMasker.maskAccountNumber("01312345678")).isEqualTo("****5678");
            assertThat(SensitiveDataMasker.maskAccountNumber("00012345678901")).isEqualTo("****8901");
        }
    }

    // ── maskPhone ────────────────────────────────────────────

    @Nested
    @DisplayName("maskPhone")
    class MaskPhone {

        @Test
        @DisplayName("null 回傳 null")
        void null_returnsNull() {
            assertThat(SensitiveDataMasker.maskPhone(null)).isNull();
        }

        @Test
        @DisplayName("長度不足 7 回傳原值")
        void shortString_returnsOriginal() {
            assertThat(SensitiveDataMasker.maskPhone("091234")).isEqualTo("091234");
        }

        @Test
        @DisplayName("正常 10 碼手機回傳前三後三遮罩")
        void normalPhone_returnsMasked() {
            assertThat(SensitiveDataMasker.maskPhone("0912345678")).isEqualTo("091***678");
        }
    }

    // ── maskEmail ────────────────────────────────────────────

    @Nested
    @DisplayName("maskEmail")
    class MaskEmail {

        @Test
        @DisplayName("null 回傳 null")
        void null_returnsNull() {
            assertThat(SensitiveDataMasker.maskEmail(null)).isNull();
        }

        @Test
        @DisplayName("不含 @ 回傳 ***")
        void noAtSign_returnsStars() {
            assertThat(SensitiveDataMasker.maskEmail("notanemail")).isEqualTo("***");
        }

        @Test
        @DisplayName("正常 Email 保留首字與 domain")
        void normalEmail_returnsMasked() {
            assertThat(SensitiveDataMasker.maskEmail("lynn@example.com")).isEqualTo("l***@example.com");
        }

        @Test
        @DisplayName("local part 單字元仍保留首字")
        void singleCharLocal_returnsMasked() {
            assertThat(SensitiveDataMasker.maskEmail("a@example.com")).isEqualTo("a***@example.com");
        }
    }
}
