package com.example.novaledger.finance.importjob.service;

import com.example.novaledger.finance.enums.ImportStatus;
import com.example.novaledger.finance.importjob.entity.UploadJob;
import com.example.novaledger.finance.importjob.parser.ParserRegistry;
import com.example.novaledger.finance.importjob.repository.UploadFileRepository;
import com.example.novaledger.finance.importjob.repository.UploadJobRepository;
import com.example.novaledger.finance.importrecord.entity.ParsedRecord;
import com.example.novaledger.finance.importrecord.repository.ImportLogRepository;
import com.example.novaledger.finance.importrecord.repository.ParsedRecordRepository;
import com.example.novaledger.finance.transaction.entity.Transaction;
import com.example.novaledger.finance.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock
    private UploadJobRepository uploadJobRepository;

    @Mock
    private ParsedRecordRepository parsedRecordRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private UploadFileRepository uploadFileRepository;

    @Mock
    private FileParserService fileParserService;

    @Mock
    private ImportLogRepository importLogRepository;

    @Mock
    private ParserRegistry parserRegistry;

    @Mock
    private ImportJobStatusService importJobStatusService;

    @Mock
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @InjectMocks
    private ImportService importService;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long JOB_ID = 100L;
    private static final Long ACCOUNT_ID = 1L;

    @Test
    void should_create_transactions_and_mark_imported_when_confirm() {
        // arrange
        UploadJob job = new UploadJob();
        job.setTenantId(TENANT_ID);
        job.setAccountId(ACCOUNT_ID);

        ParsedRecord record1 = buildRecord(-500, "中信卡");
        ParsedRecord record2 = buildRecord(32000, "跨行轉");

        when(uploadJobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(job));
        when(parsedRecordRepository.findByUploadJobIdAndTenantIdAndImportStatus(
                JOB_ID, TENANT_ID, ImportStatus.PENDING))
                .thenReturn(List.of(record1, record2));
        when(transactionService.createTransaction(any(), anyList()))
                .thenReturn(new Transaction());

        // act
        importService.confirmImport(JOB_ID, TENANT_ID, USER_ID);

        // assert：建立兩筆 transaction
        verify(transactionService, times(2)).createTransaction(any(), anyList());

        // assert：兩筆 record 都改成 IMPORTED
        assertThat(record1.getImportStatus()).isEqualTo(ImportStatus.IMPORTED);
        assertThat(record2.getImportStatus()).isEqualTo(ImportStatus.IMPORTED);
        verify(parsedRecordRepository, times(2)).save(any());
    }

    @Test
    void should_set_expense_when_amount_is_negative() {
        // arrange
        UploadJob job = new UploadJob();
        job.setTenantId(TENANT_ID);
        job.setAccountId(ACCOUNT_ID);

        ParsedRecord record = buildRecord(-678, "中信卡");

        when(uploadJobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(job));
        when(parsedRecordRepository.findByUploadJobIdAndTenantIdAndImportStatus(
                JOB_ID, TENANT_ID, ImportStatus.PENDING))
                .thenReturn(List.of(record));
        when(transactionService.createTransaction(any(), anyList()))
                .thenReturn(new Transaction());

        // act
        importService.confirmImport(JOB_ID, TENANT_ID, USER_ID);

        // assert txTypeCode = EXPENSE，totalAmount = 678
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionService).createTransaction(captor.capture(), anyList());
        assertThat(captor.getValue().getTxTypeCode()).isEqualTo("EXPENSE");
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(678));
    }

    @Test
    void should_set_income_when_amount_is_positive() {
        // arrange
        UploadJob job = new UploadJob();
        job.setTenantId(TENANT_ID);
        job.setAccountId(ACCOUNT_ID);

        ParsedRecord record = buildRecord(49000, "跨行轉");

        when(uploadJobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(job));
        when(parsedRecordRepository.findByUploadJobIdAndTenantIdAndImportStatus(
                JOB_ID, TENANT_ID, ImportStatus.PENDING))
                .thenReturn(List.of(record));
        when(transactionService.createTransaction(any(), anyList()))
                .thenReturn(new Transaction());

        // act
        importService.confirmImport(JOB_ID, TENANT_ID, USER_ID);

        // assert txTypeCode = INCOME，totalAmount = 49000
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionService).createTransaction(captor.capture(), anyList());
        assertThat(captor.getValue().getTxTypeCode()).isEqualTo("INCOME");
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(49000));
    }

    @Test
    void should_do_nothing_when_no_pending_records() {
        // arrange
        UploadJob job = new UploadJob();
        job.setTenantId(TENANT_ID);
        job.setAccountId(ACCOUNT_ID);

        when(uploadJobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(job));
        when(parsedRecordRepository.findByUploadJobIdAndTenantIdAndImportStatus(
                JOB_ID, TENANT_ID, ImportStatus.PENDING))
                .thenReturn(List.of());

        // act
        importService.confirmImport(JOB_ID, TENANT_ID, USER_ID);

        // assert：冪等性保護，不呼叫 createTransaction
        verify(transactionService, never()).createTransaction(any(), anyList());
        verify(parsedRecordRepository, never()).save(any());
    }

    // ─── helper ────────────────────────────────────────────────────

    private ParsedRecord buildRecord(long amount, String description) {
        ParsedRecord record = new ParsedRecord();
        record.setTenantId(TENANT_ID);
        record.setAmount(BigDecimal.valueOf(amount));
        record.setDescription(description);
        record.setTransactionDate(LocalDate.now());
        record.setCurrencyCode("TWD");
        record.setImportStatus(ImportStatus.PENDING);
        return record;
    }
}