package com.example.novaledger.finance.importjob.parser;

import com.example.novaledger.finance.bank.repository.BankFileFormatRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class CtbcBankStatementParser extends AbstractColumnBasedStatementParser {

    private static final String BANK_CODE = "822";
    private static final String FILE_TYPE = "CSV";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/M/d");

    public CtbcBankStatementParser(BankFileFormatRepository bankFileFormatRepository) {
        super(bankFileFormatRepository);
    }

    @Override public String getBankCode()  { return BANK_CODE; }
    @Override public String getFileType()  { return FILE_TYPE; }

    @Override protected int getDataStartRow()      { return 4; }
    @Override protected int getMinColumnCount()    { return 5; }
    @Override protected int getDateColumn()        { return 0; }
    @Override protected int getDescriptionColumn() { return 1; }
    @Override protected int getWithdrawalColumn()  { return 2; }
    @Override protected int getDepositColumn()     { return 3; }
    @Override protected int getBalanceColumn()     { return 4; }

    @Override
    protected LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    @Override
    public boolean supportsFormatDetection() {
        return true;
    }

    @Override
    public boolean canHandle(List<List<String>> rows) {
        // header 在 index 3，檢查關鍵欄位名稱
        if (rows == null || rows.size() < 4) return false;
        List<String> header = rows.get(3);
        if (header.size() < 5) return false;
        return header.get(0).contains("日期")
                && header.get(1).contains("摘要")
                && header.get(2).contains("支出")
                && header.get(3).contains("存入")
                && header.get(4).contains("結餘");
    }
}
