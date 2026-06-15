package com.example.novaledger.finance.importjob.util;

import com.example.novaledger.finance.bank.entity.BankFileFormat;
import com.example.novaledger.finance.bank.repository.BankFileFormatRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 開發工具：為新 parser 產生不衝突的 parserKey 並寫入 bank_file_formats
 * 格式：{BANK_CODE}_{FILE_TYPE}_{YYYYMMDD}，如 807_CSV_20250401
 * 若當日 key 已存在，自動日期 +1 天直到不衝突
 *
 * 使用方式：開發新 parser 時手動呼叫一次 generate()
 * 回傳值為寫入 DB 的 BankFileFormat entity，其 parserKey 即為 parser class 應使用的值
 */
@Component
public class ParserKeyGenerator {

    private final BankFileFormatRepository bankFileFormatRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public ParserKeyGenerator(BankFileFormatRepository bankFileFormatRepository) {
        this.bankFileFormatRepository = bankFileFormatRepository;
    }

    public BankFileFormat generate(String bankCode, String fileType, String description) {
        LocalDate date = LocalDate.now();
        String candidate = buildKey(bankCode, fileType, date);

        // 若 key 已存在，日期往後推直到不衝突
        while (bankFileFormatRepository.existsByParserKey(candidate)) {
            date = date.plusDays(1);
            candidate = buildKey(bankCode, fileType, date);
        }

        BankFileFormat format = new BankFileFormat();
        format.setBankCode(bankCode);
        format.setFileType(fileType);
        format.setReleasedDate(date);
        format.setParserKey(candidate);
        format.setIsActive(1);

        return bankFileFormatRepository.save(format);
    }

    private String buildKey(String bankCode, String fileType, LocalDate date) {
        return bankCode + "_" + fileType + "_" + date.format(FORMATTER);
    }
}
