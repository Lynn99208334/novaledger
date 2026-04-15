package com.example.novaledger.finance.importjob.parser;

import java.util.List;

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
     * 判斷此 parser 是否能處理指定的原始列資料
     * 用於自動偵測（MVP 後啟用）
     */
    boolean canHandle(List<List<String>> rows);

    /**
     * 解析所有資料列，回傳解析結果清單
     * @param rows 已讀取的所有列（含 header）
     */
    List<ParseResult> parse(List<List<String>> rows);
}