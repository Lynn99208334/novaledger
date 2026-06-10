package com.example.novaledger.finance.importjob.util;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

@Component
public class MimeTypeValidator {

    private static final Logger log = LoggerFactory.getLogger(MimeTypeValidator.class);

    // Tika 是 thread-safe，直接共用同一個 instance
    private static final Tika TIKA = new Tika();

    // 允許的副檔名 → 對應合法的 MIME types
    // xlsx 本質是 zip，Tika 對部分銀行匯出的 xlsx 會偵測為 application/zip
    // xls 是 OLE2 container，Tika 有時回傳 application/x-tika-msoffice
    private static final Map<String, Set<String>> ALLOWED_MIME_BY_EXT = Map.of(
            "csv", Set.of(
                    "text/plain",
                    "text/csv",
                    "application/csv",
                    // Big5 / 非 UTF-8 編碼的 CSV（台灣銀行常見）
                    // Tika 無法識別編碼時統一回傳 application/octet-stream
                    // tradeoff：此值也涵蓋所有 Tika 辨識不出的格式，
                    // 但 CSV 無 magic byte 本就無法靠 bytes 嚴格驗證，屬已知限制
                    "application/octet-stream"
            ),
            "xlsx", Set.of(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/zip"  // fallback: 部分 xlsx 被 Tika 偵測為 zip
            ),
            "xls", Set.of(
                    "application/vnd.ms-excel",
                    "application/x-tika-msoffice"  // fallback: OLE2 container
            )
    );

    /**
     * 驗證上傳檔案的真實 MIME type 是否與副檔名相符。
     * 防止攻擊者將惡意檔案改名成 .csv / .xlsx 上傳。
     *
     * @param fileBytes        檔案內容（已從 MultipartFile 讀出）
     * @param originalFilename 原始檔名（含副檔名）
     * @throws BusinessException FILE_INVALID              副檔名缺失
     * @throws BusinessException FILE_TYPE_NOT_SUPPORTED   副檔名不在允許清單
     * @throws BusinessException FILE_TYPE_MISMATCH        真實 MIME type 與副檔名不符
     */
    public void validate(byte[] fileBytes, String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }

        String extension = originalFilename
                .substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();

        Set<String> allowedMimes = ALLOWED_MIME_BY_EXT.get(extension);
        if (allowedMimes == null) {
            log.warn("action=MIME_VALIDATE result=FAILED reason=EXTENSION_NOT_SUPPORTED filename={} extension={}",
                    originalFilename, extension);
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        String detectedMime;
        try {
            detectedMime = TIKA.detect(fileBytes);
        } catch (Exception e) {
            log.error("action=MIME_VALIDATE result=FAILED reason=TIKA_ERROR filename={}", originalFilename, e);
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }

        if (!allowedMimes.contains(detectedMime)) {
            // 擷取前 8 bytes 的 hex，方便資安事件調查時確認攻擊者送了什麼
            String hexPrefix = bytesToHex(Arrays.copyOf(fileBytes, Math.min(fileBytes.length, 8)));
            log.warn("action=MIME_VALIDATE result=FAILED filename={} extension={} detectedMime={} hexPrefix={}",
                    originalFilename, extension, detectedMime, hexPrefix);
            throw new BusinessException(ErrorCode.FILE_TYPE_MISMATCH);
        }

        log.info("action=MIME_VALIDATE result=OK filename={} extension={} detectedMime={}",
                originalFilename, extension, detectedMime);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
