package com.example.novaledger.finance.importjob.parser;

import com.example.novaledger.finance.bank.repository.BankFileFormatRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class YongfengBankStatementParser extends AbstractColumnBasedStatementParser {

    private static final String BANK_CODE = "807";
    private static final String FILE_TYPE = "CSV";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    public YongfengBankStatementParser(BankFileFormatRepository bankFileFormatRepository) {
        super(bankFileFormatRepository);
    }

    @Override public String getBankCode()  { return BANK_CODE; }
    @Override public String getFileType()  { return FILE_TYPE; }

    @Override protected int getDataStartRow()      { return 5; }
    @Override protected int getMinColumnCount()    { return 6; }
    @Override protected int getDateColumn()        { return 0; }
    @Override protected int getDescriptionColumn() { return 2; }
    @Override protected int getWithdrawalColumn()  { return 3; }
    @Override protected int getDepositColumn()     { return 4; }
    @Override protected int getBalanceColumn()     { return 5; }

    @Override
    protected LocalDate parseDate(String dateStr) {
        return LocalDateTime.parse(dateStr, DATE_FORMATTER).toLocalDate();
    }

    @Override
    public boolean canHandle(List<List<String>> rows) {
        return false;
    }
}
