package com.example.novaledger.finance.importjob.parser;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.finance.bank.repository.BankFileFormatRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractColumnBasedStatementParser implements BankStatementParser {

    private final BankFileFormatRepository bankFileFormatRepository;

    protected AbstractColumnBasedStatementParser(BankFileFormatRepository bankFileFormatRepository) {
        this.bankFileFormatRepository = bankFileFormatRepository;
    }

    // ── 子類別必須實作的 hook ──────────────────────────────────────

    /** 資料列從第幾個 index 開始（0-based，含 header 的 row 不解析） */
    protected abstract int getDataStartRow();

    /** 最少需要幾欄才算合法列 */
    protected abstract int getMinColumnCount();

    protected abstract int getDateColumn();
    protected abstract int getDescriptionColumn();
    protected abstract int getWithdrawalColumn();
    protected abstract int getDepositColumn();
    protected abstract int getBalanceColumn();

    /**
     * 子類別自行決定日期格式與解析方式。
     * 若格式不符應拋出 DateTimeParseException 或 IllegalArgumentException。
     */
    protected abstract LocalDate parseDate(String dateStr);

    // ── 骨架實作（子類別不需要 override）──────────────────────────

    /**
     * 從 DB 撈此 parser 對應的最新版 parserKey
     * 同一銀行同一 fileType 可能有多個版本，取 releasedDate 最新的啟用版本
     */
    @Override
    public String getParserKey() {
        return bankFileFormatRepository
                .findTopByBankCodeAndFileTypeAndIsActiveOrderByReleasedDateDesc(
                        getBankCode(), getFileType(), 1)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMPORT_PARSER_NOT_FOUND))
                .getParserKey();
    }

    @Override
    public List<ParseResult> parse(List<List<String>> rows) {
        List<ParseResult> results = new ArrayList<>();
        for (int i = getDataStartRow(); i < rows.size(); i++) {
            List<String> row = rows.get(i);
            // 跳過空列
            if (row.stream().allMatch(cell -> cell.trim().isEmpty())) {
                continue;
            }
            results.add(parseRow(row, i + 1));
        }
        return results;
    }

    private ParseResult parseRow(List<String> rowData, int rowNumber) {
        try {
            if (rowData.size() < getMinColumnCount()) {
                return ParseResult.failure(
                        "第 " + rowNumber + " 列欄位數不足，期望 " + getMinColumnCount()
                        + " 欄，實際 " + rowData.size(),
                        rowNumber, rowData);
            }

            String dateStr       = rowData.get(getDateColumn()).trim();
            String description   = rowData.get(getDescriptionColumn()).trim();
            String withdrawalStr = rowData.get(getWithdrawalColumn()).trim();
            String depositStr    = rowData.get(getDepositColumn()).trim();
            String balanceStr    = rowData.get(getBalanceColumn()).trim();

            if (dateStr.isEmpty()) {
                return ParseResult.failure(
                        "第 " + rowNumber + " 列日期欄位為空", rowNumber, rowData);
            }

            LocalDate date;
            try {
                date = parseDate(dateStr);
            } catch (Exception e) {
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
                            "第 " + rowNumber + " 列支出存入欄位皆為空", rowNumber, rowData);
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
