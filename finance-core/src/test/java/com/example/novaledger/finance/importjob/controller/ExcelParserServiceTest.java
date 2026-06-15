package com.example.novaledger.finance.importjob.controller;

import com.example.novaledger.finance.bank.repository.BankFileFormatRepository;
import com.example.novaledger.finance.importjob.parser.CtbcBankStatementParser;
import com.example.novaledger.finance.importjob.parser.ParseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class CtbcBankStatementParserTest {

    @Mock
    private BankFileFormatRepository bankFileFormatRepository;

    private CtbcBankStatementParser parser;

    // header 列（4 列）+ 資料列，DATA_START_ROW = 4
    private static final List<String> EMPTY_ROW = List.of("", "", "", "", "");
    private static final List<List<String>> HEADER_ROWS = List.of(
            EMPTY_ROW, EMPTY_ROW, EMPTY_ROW, EMPTY_ROW
    );

    @BeforeEach
    void setUp() {
        parser = new CtbcBankStatementParser(bankFileFormatRepository);
    }

    private List<ParseResult> parseOneRow(List<String> dataRow) {
        List<List<String>> rows = new java.util.ArrayList<>(HEADER_ROWS);
        rows.add(dataRow);
        return parser.parse(rows);
    }

    @Test
    @DisplayName("正常支出列 → 解析成功，amount 為負值")
    void parse_validWithdrawal_success() {
        List<ParseResult> results = parseOneRow(
                List.of("2026/3/3", "中信卡", "678", "", "0"));

        assertThat(results.size()).isEqualTo(1);
        ParseResult result = results.get(0);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionDate()).isEqualTo(LocalDate.of(2026, 3, 3));
        assertThat(result.getDescription()).isEqualTo("中信卡");
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("-678"));
        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("0"));
    }

    @Test
    @DisplayName("正常存入列 → 解析成功，amount 為正值")
    void parse_validDeposit_success() {
        List<ParseResult> results = parseOneRow(
                List.of("2026/3/3", "跨行轉", "", "49000", "49000"));

        assertThat(results.size()).isEqualTo(1);
        ParseResult result = results.get(0);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("49000"));
        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("49000"));
    }

    @Test
    @DisplayName("金額格式錯誤 → 解析失敗，errorMessage 包含關鍵字")
    void parse_invalidAmount_failure() {
        List<ParseResult> results = parseOneRow(
                List.of("2026/3/3", "中信卡", "abc", "", "0"));

        assertThat(results.size()).isEqualTo(1);
        ParseResult result = results.get(0);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("金額格式錯誤");
    }

    @Test
    @DisplayName("日期格式錯誤 → 解析失敗，errorMessage 包含關鍵字")
    void parse_invalidDate_failure() {
        List<ParseResult> results = parseOneRow(
                List.of("20260303", "中信卡", "678", "", "0"));

        assertThat(results.size()).isEqualTo(1);
        ParseResult result = results.get(0);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("日期格式錯誤");
    }

    @Test
    @DisplayName("欄位數不足 → 解析失敗")
    void parse_insufficientColumns_failure() {
        List<ParseResult> results = parseOneRow(
                List.of("2026/3/3", "中信卡"));

        assertThat(results.size()).isEqualTo(1);
        ParseResult result = results.get(0);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("欄位數不足");
    }
}
