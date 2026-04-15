package com.example.novaledger.finance.importjob.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class EsunBankStatementParser implements BankStatementParser {

    private static final String PARSER_KEY     = "808_CSV_20250401";
    private static final String BANK_CODE      = "808";
    private static final String FILE_TYPE      = "CSV";
    private static final int    DATA_START_ROW = 1;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public String getParserKey() { return PARSER_KEY; }

    @Override
    public String getBankCode() { return BANK_CODE; }

    @Override
    public String getFileType() { return FILE_TYPE; }

    @Override
    public boolean canHandle(List<List<String>> rows) {
        return false;
    }

    @Override
    public List<ParseResult> parse(List<List<String>> rows) {
        List<ParseResult> results = new ArrayList<>();
        for (int i = DATA_START_ROW; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.stream().allMatch(cell -> cell.trim().isEmpty())) {
                continue;
            }
            results.add(parseRow(row, i + 1));
        }
        return results;
    }

    private ParseResult parseRow(List<String> rowData, int rowNumber) {
        try {
            if (rowData.size() < 6) {
                return ParseResult.failure(
                        "第 " + rowNumber + " 列欄位數不足，期望 6 欄，實際 " + rowData.size(),
                        rowNumber, rowData);
            }

            String dateStr       = rowData.get(0).trim();
            String description   = rowData.get(2).trim();
            String withdrawalStr = rowData.get(3).trim();
            String depositStr    = rowData.get(4).trim();
            String balanceStr    = rowData.get(5).trim();

            if (dateStr.isEmpty()) {
                return ParseResult.failure(
                        "第 " + rowNumber + " 列日期欄位為空", rowNumber, rowData);
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                return ParseResult.failure(
                        "第 " + rowNumber + " 列日期格式錯誤：" + dateStr, rowNumber, rowData);
            }

            BigDecimal amount;
            try {
                if (!withdrawalStr.isEmpty()) {
                    amount = new BigDecimal(withdrawalStr.replace(",", "")).negate();
                } else if (!depositStr.isEmpty()) {
                    amount = new BigDecimal(depositStr.replace(",", ""));
                } else {
                    return ParseResult.failure(
                            "第 " + rowNumber + " 列提存欄位皆為空", rowNumber, rowData);
                }
            } catch (NumberFormatException e) {
                return ParseResult.failure(
                        "第 " + rowNumber + " 列金額格式錯誤：" + withdrawalStr + "/" + depositStr,
                        rowNumber, rowData);
            }

            BigDecimal balance;
            try {
                balance = new BigDecimal(balanceStr.replace(",", "").replace("'", ""));
            } catch (NumberFormatException e) {
                return ParseResult.failure(
                        "第 " + rowNumber + " 列餘額格式錯誤：" + balanceStr, rowNumber, rowData);
            }

            return ParseResult.success(date, description, amount, balance, rowNumber, rowData);

        } catch (Exception e) {
            return ParseResult.failure(
                    "第 " + rowNumber + " 列解析失敗：" + e.getMessage(), rowNumber, rowData);
        }
    }
}