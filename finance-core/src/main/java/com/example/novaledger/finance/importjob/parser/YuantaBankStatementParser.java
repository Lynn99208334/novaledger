package com.example.novaledger.finance.importjob.parser;

import com.example.novaledger.finance.bank.repository.BankFileFormatRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class YuantaBankStatementParser extends AbstractColumnBasedStatementParser {

    private static final String BANK_CODE = "806";
    private static final String FILE_TYPE = "CSV";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    public YuantaBankStatementParser(BankFileFormatRepository bankFileFormatRepository) {
        super(bankFileFormatRepository);
    }

    @Override public String getBankCode()  { return BANK_CODE; }
    @Override public String getFileType()  { return FILE_TYPE; }

    @Override protected int getDataStartRow()      { return 1; }
    @Override protected int getMinColumnCount()    { return 8; }
    @Override protected int getDateColumn()        { return 2; }
    @Override protected int getDescriptionColumn() { return 4; }
    @Override protected int getWithdrawalColumn()  { return 5; }
    @Override protected int getDepositColumn()     { return 6; }
    @Override protected int getBalanceColumn()     { return 7; }

    @Override
    protected LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
    }

    /**
     * 元大帳號在每列資料的 index 0，格式為 '20872000150658'（含前導單引號）
     * 取第一筆資料列的帳號欄位，去除前導單引號後回傳
     */
    @Override
    public Optional<String> extractAccountNumber(List<List<String>> rows) {
        for (int i = getDataStartRow(); i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.size() > 0) {
                String raw = row.get(0).trim();
                if (!raw.isEmpty()) {
                    // 去除 CSV 防呆用的前導單引號
                    return Optional.of(raw.startsWith("'") ? raw.substring(1) : raw);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean canHandle(List<List<String>> rows) {
        if (rows == null || rows.size() < 1) return false;
        List<String> header = rows.get(0);
        if (header.size() < 2) return false;
        // 元大 header 特徵：第1欄「帳號」第2欄「帳值日期」
        String col0 = header.get(0).trim();
        String col1 = header.get(1).trim();
        return col0.contains("帳號") && col1.contains("帳值日期");
    }
}
