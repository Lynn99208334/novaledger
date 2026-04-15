package com.example.novaledger.finance.importjob.util;

import com.example.novaledger.finance.importjob.parser.ParserRegistry;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 開發工具：產生不衝突的 parserKey
 * 格式：{BANK_CODE}_{FILE_TYPE}_{YYYYMMDD}
 * 若當日已有相同 key，自動日期 +1 天
 *
 * 使用方式：手動呼叫 generate()，將結果填入 parser 的 PARSER_KEY 常數
 */
@Component
public class ParserKeyGenerator {

    private final ParserRegistry parserRegistry;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    public ParserKeyGenerator(ParserRegistry parserRegistry) {
        this.parserRegistry = parserRegistry;
    }

    public String generate(String bankCode, String fileType) {
        LocalDate date = LocalDate.now();
        String candidate = buildKey(bankCode, fileType, date);

        // 若 key 已存在，日期往後推直到不衝突
        while (keyExists(candidate)) {
            date = date.plusDays(1);
            candidate = buildKey(bankCode, fileType, date);
        }
        return candidate;
    }

    private String buildKey(String bankCode, String fileType, LocalDate date) {
        return bankCode + "_" + fileType + "_" + date.format(FORMATTER);
    }

    private boolean keyExists(String key) {
        try {
            parserRegistry.getParser(key);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}