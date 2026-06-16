package com.example.novaledger.service;

import com.example.novaledger.dto.ParserOverviewDto;
import com.example.novaledger.finance.bank.repository.BankRepository;
import com.example.novaledger.finance.importjob.parser.BankStatementParser;
import com.example.novaledger.finance.importjob.parser.ParserRegistry;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminParserService {

    private final ParserRegistry parserRegistry;
    private final BankRepository bankRepository;

    public AdminParserService(ParserRegistry parserRegistry, BankRepository bankRepository) {
        this.parserRegistry = parserRegistry;
        this.bankRepository = bankRepository;
    }

    public List<ParserOverviewDto> getParserOverview() {
        // 撈所有銀行名稱（bankCode → name）
        Map<String, String> bankNames = bankRepository.findAll().stream()
                .collect(Collectors.toMap(
                        b -> b.getBankCode(),
                        b -> b.getName(),
                        (a, b) -> a
                ));

        List<BankStatementParser> parsers = parserRegistry.getAllParsers();

        return parsers.stream()
                .map(p -> ParserOverviewDto.builder()
                        .bankCode(p.getBankCode())
                        .bankName(bankNames.getOrDefault(p.getBankCode(), "（未知銀行）"))
                        .fileType(p.getFileType())
                        .parserKey(p.getParserKey())
                        .supportsAutoDetect(p.supportsFormatDetection())
                        .supportsAccountExtraction(p.extractAccountNumber(Collections.emptyList()).isPresent()
                                || isOverrideExtractAccountNumber(p))
                        .build())
                .sorted((a, b) -> a.getBankCode().compareTo(b.getBankCode()))
                .collect(Collectors.toList());
    }

    /**
     * 判斷 parser 是否有覆寫 extractAccountNumber()。
     * 用空 list 呼叫並不準確（空 list 永遠 empty），
     * 改用 class 層級判斷是否覆寫了 default method。
     */
    private boolean isOverrideExtractAccountNumber(BankStatementParser parser) {
        try {
            parser.getClass().getDeclaredMethod("extractAccountNumber", java.util.List.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
