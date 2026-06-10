package com.example.novaledger.finance.importjob.util;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MimeTypeValidatorTest {

    private final MimeTypeValidator validator = new MimeTypeValidator();

    // xlsx 的 magic byte（PK zip header）
    // Tika 看到 50 4B 03 04 會判斷為 application/zip 或 xlsx，屬於允許清單
    private static final byte[] XLSX_MAGIC = new byte[]{
            0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00
    };

    // xls 的 magic byte（OLE2 Compound Document header）
    // Tika 看到 D0 CF 11 E0 會判斷為 application/vnd.ms-excel 或 x-tika-msoffice
    private static final byte[] XLS_MAGIC = new byte[]{
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };

    // PNG 的 magic byte - 不應被接受為任何允許格式
    private static final byte[] PNG_MAGIC = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    // ===================== CSV =====================

    @Test
    void UTF8編碼的CSV_應通過驗證() {
        byte[] utf8Csv = "日期,金額,摘要\n2024-01-01,1000,測試轉帳".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertThatNoException().isThrownBy(() -> validator.validate(utf8Csv, "statement.csv"));
    }

    @Test
    void Big5編碼的CSV_應通過驗證() {
        // 模擬台灣銀行匯出的 Big5 CSV，Tika 會回傳 application/octet-stream
        byte[] big5Csv = new byte[]{(byte) 0xAC, (byte) 0xA1, (byte) 0xA6, 0x73, 0x2C, 0x31, 0x30, 0x30};
        assertThatNoException().isThrownBy(() -> validator.validate(big5Csv, "bank_export.csv"));
    }

    @Test
    void PNG改名成CSV_應拋出FILE_TYPE_MISMATCH() {
        assertThatThrownBy(() -> validator.validate(PNG_MAGIC, "fake.csv"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TYPE_MISMATCH);
    }

    // ===================== XLSX =====================

    @Test
    void 合法xlsx_應通過驗證() {
        assertThatNoException().isThrownBy(() -> validator.validate(XLSX_MAGIC, "statement.xlsx"));
    }

    @Test
    void 純文字改名成xlsx_應拋出FILE_TYPE_MISMATCH() {
        byte[] fakeXlsx = "this is plain text, not xlsx".getBytes();
        assertThatThrownBy(() -> validator.validate(fakeXlsx, "fake.xlsx"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TYPE_MISMATCH);
    }

    @Test
    void PNG改名成xlsx_應拋出FILE_TYPE_MISMATCH() {
        assertThatThrownBy(() -> validator.validate(PNG_MAGIC, "fake.xlsx"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TYPE_MISMATCH);
    }

    // ===================== XLS =====================

    @Test
    void 合法xls_應通過驗證() {
        assertThatNoException().isThrownBy(() -> validator.validate(XLS_MAGIC, "statement.xls"));
    }

    @Test
    void PNG改名成xls_應拋出FILE_TYPE_MISMATCH() {
        assertThatThrownBy(() -> validator.validate(PNG_MAGIC, "fake.xls"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TYPE_MISMATCH);
    }

    // ===================== 副檔名不支援 =====================

    @Test
    void 不支援的副檔名exe_應拋出FILE_TYPE_NOT_SUPPORTED() {
        assertThatThrownBy(() -> validator.validate(PNG_MAGIC, "malware.exe"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
    }

    @Test
    void 不支援的副檔名pdf_應拋出FILE_TYPE_NOT_SUPPORTED() {
        byte[] pdfMagic = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34}; // %PDF-1.4
        assertThatThrownBy(() -> validator.validate(pdfMagic, "document.pdf"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
    }

    // ===================== 邊界條件 =====================

    @Test
    void 檔名為null_應拋出FILE_INVALID() {
        assertThatThrownBy(() -> validator.validate("any".getBytes(), null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_INVALID);
    }

    @Test
    void 檔名無副檔名_應拋出FILE_INVALID() {
        assertThatThrownBy(() -> validator.validate("any".getBytes(), "nodotfilename"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_INVALID);
    }
}
