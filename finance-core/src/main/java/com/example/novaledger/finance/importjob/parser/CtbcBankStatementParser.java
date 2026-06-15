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
    public boolean canHandle(List<List<String>> rows) {
        return false;
    }
}
