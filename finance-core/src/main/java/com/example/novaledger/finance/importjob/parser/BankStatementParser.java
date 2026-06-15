package com.example.novaledger.finance.importjob.parser;

import java.util.List;
import java.util.Optional;

public interface BankStatementParser {

    /**
     * 回傳此 parser 對應的 parser_key
     * 格式：{BANK_CODE}_{FILE_TYPE}_{YYYYMMDD}
     * 範例：822_CSV_20250401
     */
    String getParserKey();

    /**
     * 回傳銀行代碼，例如 "822"
     */
    String getBankCode();

    /**
     * 回傳檔案類型，例如 "CSV"、"XLSX"
     */
    String getFileType();

    /**
     * 是否支援格式自動偵測
     * 有實作 canHandle() 的 parser 覆寫此方法回傳 true
     */
    default boolean supportsFormatDetection() {
        return false;
    }

    /**
     * 判斷此 parser 是否能處理指定的原始列資料
     * 用於自動偵測（MVP 後啟用）
     */
    boolean canHandle(List<List<String>> rows);

    /**
     * 解析所有資料列，回傳解析結果清單
     * @param rows 已讀取的所有列（含 header）
     */
    List<ParseResult> parse(List<List<String>> rows);

    /**
     * 從檔案內容中擷取帳號
     * 用於比對使用者選定的帳戶，不一致時擋下匯入
     * 無法擷取時回傳 Optional.empty()，視為無法驗證、放行
     */
    default Optional<String> extractAccountNumber(List<List<String>> rows) {
        return Optional.empty();
    }
}
