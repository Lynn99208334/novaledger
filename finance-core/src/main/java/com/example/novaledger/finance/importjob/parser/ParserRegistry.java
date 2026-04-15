package com.example.novaledger.finance.importjob.parser;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ParserRegistry {

    private final Map<String, BankStatementParser> parsersByKey;

    public ParserRegistry(List<BankStatementParser> parsers) {
        this.parsersByKey = parsers.stream()
                .collect(Collectors.toMap(BankStatementParser::getParserKey, p -> p));
    }

    /**
     * 用 parserKey 直接取得 parser
     */
    public BankStatementParser getParser(String parserKey) {
        BankStatementParser parser = parsersByKey.get(parserKey);
        if (parser == null) {
            throw new BusinessException(ErrorCode.IMPORT_PARSER_NOT_FOUND);
        }
        return parser;
    }

    /**
     * 自動偵測（MVP 後實作）
     * 目前保留介面，實作留空
     */
    public BankStatementParser detectParser(String bankCode, List<List<String>> rows) {
        throw new BusinessException(ErrorCode.IMPORT_PARSER_NOT_FOUND);
    }

    public String resolveParserKey(String bankCode, String fileName) {
        String fileType = fileName.toLowerCase().endsWith(".csv") ? "CSV" : "XLSX";
        return parsersByKey.values().stream()
                .filter(p -> p.getBankCode().equals(bankCode)
                        && p.getFileType().equals(fileType))
                .map(BankStatementParser::getParserKey)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.IMPORT_PARSER_NOT_FOUND));
    }
}