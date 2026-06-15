package com.example.novaledger.finance.importjob.service;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.common.logging.AuditLog;
import com.example.novaledger.common.logging.AuditType;
import com.example.novaledger.finance.enums.ImportStatus;
import com.example.novaledger.finance.enums.ParseStatus;
import com.example.novaledger.finance.importjob.dto.ImportSummary;
import com.example.novaledger.finance.importjob.dto.JobStatusResponse;
import com.example.novaledger.finance.importjob.dto.UploadJobResponse;
import com.example.novaledger.finance.importjob.entity.UploadFile;
import com.example.novaledger.finance.importjob.entity.UploadJob;
import com.example.novaledger.finance.importjob.parser.ParseResult;
import com.example.novaledger.finance.importjob.parser.ParserRegistry;
import com.example.novaledger.finance.importjob.util.MimeTypeValidator;
import com.example.novaledger.finance.importjob.repository.UploadFileRepository;
import com.example.novaledger.finance.importjob.repository.UploadJobRepository;
import com.example.novaledger.finance.importrecord.dto.ParsedRecordErrorResponse;
import com.example.novaledger.finance.importrecord.dto.ParsedRecordPreviewResponse;
import com.example.novaledger.finance.importrecord.entity.ImportLog;
import com.example.novaledger.finance.importrecord.entity.ParsedRecord;
import com.example.novaledger.finance.importrecord.repository.ImportLogRepository;
import com.example.novaledger.finance.importrecord.repository.ParsedRecordRepository;
import com.example.novaledger.finance.transaction.entity.Transaction;
import com.example.novaledger.finance.transaction.entity.TransactionItem;
import com.example.novaledger.finance.transaction.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final List<String> ALLOWED_EXTENSIONS = List.of("xlsx", "xls", "csv");

    private final UploadJobRepository uploadJobRepository;
    private final UploadFileRepository uploadFileRepository;
    private final FileParserService fileParserService;
    private final ParsedRecordRepository parsedRecordRepository;
    private final ImportLogRepository importLogRepository;
    private final ParserRegistry parserRegistry;
    private final ImportJobStatusService importJobStatusService;
    private final TransactionTemplate transactionTemplate;
    private final TransactionService transactionService;
    private final MimeTypeValidator mimeTypeValidator;

    public ImportService(UploadJobRepository uploadJobRepository,
                         UploadFileRepository uploadFileRepository,
                         FileParserService fileParserService,
                         ParsedRecordRepository parsedRecordRepository,
                         ImportLogRepository importLogRepository,
                         ParserRegistry parserRegistry,
                         ImportJobStatusService importJobStatusService,
                         TransactionTemplate transactionTemplate,
                         TransactionService transactionService,
                         MimeTypeValidator mimeTypeValidator) {
        this.uploadJobRepository = uploadJobRepository;
        this.uploadFileRepository = uploadFileRepository;
        this.fileParserService = fileParserService;
        this.parsedRecordRepository = parsedRecordRepository;
        this.importLogRepository = importLogRepository;
        this.parserRegistry = parserRegistry;
        this.importJobStatusService = importJobStatusService;
        this.transactionTemplate = transactionTemplate;
        this.transactionService = transactionService;
        this.mimeTypeValidator = mimeTypeValidator;
    }

    @Transactional
    public UploadJobResponse createUploadJob(MultipartFile file, String jobType,
                                             String bankCode, Long accountId,
                                             Long tenantId, Long userId) {
        log.info("action=CREATE_UPLOAD_JOB tenantId={} userId={} filename={} bankCode={}",
                tenantId, userId, file.getOriginalFilename(), bankCode);
        validateFile(file);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("action=CREATE_UPLOAD_JOB result=FAILED reason=FILE_READ_FAILED filename={}", file.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.IMPORT_FILE_READ_FAILED);
        }

        // S10: Tika MIME type 驗證 - 防止改名攻擊
        mimeTypeValidator.validate(fileBytes, file.getOriginalFilename());

        String parserKey = parserRegistry.resolveParserKey(bankCode, file.getOriginalFilename());

        UploadJob job = new UploadJob();
        job.setTenantId(tenantId);
        job.setStatus("PENDING");
        job.setCreatedBy(userId);
        job.setJobType(jobType);
        job.setParserKey(parserKey);
        job.setAccountId(accountId);
        uploadJobRepository.save(job);

        log.info("action=CREATE_UPLOAD_JOB result=SUCCESS jobId={} parserKey={}", job.getId(), parserKey);

        UploadJobResponse response = new UploadJobResponse();
        response.setJobId(job.getId());
        response.setStatus(job.getStatus());
        response.setJobType(job.getJobType());
        response.setOriginalFilename(file.getOriginalFilename());
        response.setCreatedAt(job.getCreatedAt());

        processImportJob(job.getId(), tenantId, fileBytes,
                file.getOriginalFilename(), file.getContentType(), file.getSize());

        return response;
    }

    @AuditLog(action = "CONFIRM_IMPORT", type = AuditType.CREATE)
    @Transactional
    public ImportSummary confirmImport(Long jobId, Long tenantId, Long userId) {
        log.info("action=CONFIRM_IMPORT jobId={} tenantId={} userId={}", jobId, tenantId, userId);

        UploadJob job = uploadJobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMPORT_JOB_NOT_FOUND));

        List<ParsedRecord> pendingRecords = parsedRecordRepository
                .findByUploadJobIdAndTenantIdAndImportStatus(jobId, tenantId, ImportStatus.PENDING);

        if (pendingRecords.isEmpty()) {
            log.info("action=CONFIRM_IMPORT result=SKIPPED reason=NO_PENDING_RECORDS jobId={}", jobId);
            return new ImportSummary(jobId, 0, 0);
        }

        int importedCount = 0;
        for (ParsedRecord record : pendingRecords) {
            String txTypeCode = record.getAmount().compareTo(BigDecimal.ZERO) < 0
                    ? "EXPENSE" : "INCOME";
            BigDecimal totalAmount = record.getAmount().abs();

            Transaction tx = new Transaction();
            tx.setTenantId(tenantId);
            tx.setUserId(userId);
            tx.setAccountId(job.getAccountId());
            tx.setTxTypeCode(txTypeCode);
            tx.setTransactionDate(record.getTransactionDate());
            tx.setTotalAmount(totalAmount);
            tx.setCurrencyCode(record.getCurrencyCode());
            tx.setMemo(record.getDescription());

            TransactionItem item = new TransactionItem();
            item.setAmount(totalAmount);
            item.setMemo(record.getDescription());

            transactionService.createTransaction(tx, List.of(item));

            record.setImportStatus(ImportStatus.IMPORTED);
            parsedRecordRepository.save(record);
            importedCount++;
        }

        int skippedCount = job.getDupCount() != null ? job.getDupCount() : 0;
        log.info("action=CONFIRM_IMPORT result=SUCCESS jobId={} importedCount={} skippedCount={}", jobId, importedCount, skippedCount);
        return new ImportSummary(jobId, importedCount, skippedCount);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
        String extension = originalFilename
                .substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("action=VALIDATE_FILE result=FAILED reason=FILE_TYPE_NOT_SUPPORTED filename={}", originalFilename);
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }
    }

    @Async
    public void processImportJob(Long jobId, Long tenantId, byte[] fileBytes,
                                 String originalFilename, String mimeType, long fileSize) {
        log.info("action=PROCESS_IMPORT_JOB jobId={} tenantId={} filename={}", jobId, tenantId, originalFilename);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                UploadJob job = uploadJobRepository.findByIdAndTenantId(jobId, tenantId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.IMPORT_JOB_NOT_FOUND));

                job.setStatus("PROCESSING");
                uploadJobRepository.save(job);

                UploadFile uploadFile = new UploadFile();
                uploadFile.setUploadJobId(jobId);
                uploadFile.setTenantId(tenantId);
                uploadFile.setOriginalFilename(originalFilename);
                uploadFile.setStoredFilename("");
                uploadFile.setFileSize(fileSize);
                uploadFile.setMimeType(mimeType);
                uploadFileRepository.save(uploadFile);

                List<ParseResult> results = fileParserService.parse(
                        fileBytes, originalFilename, job.getParserKey());

                int successCount = 0;
                int failCount = 0;
                int dupCount = 0;

                for (ParseResult result : results) {
                    ParsedRecord record = new ParsedRecord();
                    record.setTenantId(tenantId);
                    record.setUploadJobId(jobId);
                    record.setUploadFileId(uploadFile.getId());
                    record.setSourceRowNum(result.getRowNumber());
                    record.setRawData(buildRawDataJson(result));

                    if (result.isSuccess()) {
                        String dedupKey = calculateDedupKey(
                                job.getAccountId(), result);

                        if (parsedRecordRepository.existsByDedupKey(dedupKey)) {
                            record.setParseStatus(ParseStatus.SUCCESS.name());
                            record.setImportStatus(ImportStatus.DUPLICATE);
                            record.setTransactionDate(result.getTransactionDate());
                            record.setDescription(result.getDescription());
                            record.setAmount(result.getAmount());
                            record.setBalance(result.getBalance());
                            record.setCurrencyCode("TWD");
                            // dedupKey 不寫入，避免違反 UNIQUE constraint
                            dupCount++;
                            log.debug("action=PARSE_RECORD result=DUPLICATE jobId={} row={} dedupKey={}",
                                    jobId, result.getRowNumber(), dedupKey);
                        } else {
                            record.setParseStatus(ParseStatus.SUCCESS.name());
                            record.setImportStatus(ImportStatus.PENDING);
                            record.setTransactionDate(result.getTransactionDate());
                            record.setDescription(result.getDescription());
                            record.setAmount(result.getAmount());
                            record.setBalance(result.getBalance());
                            record.setCurrencyCode("TWD");
                            record.setDedupKey(dedupKey);
                            successCount++;
                        }
                    } else {
                        record.setParseStatus(ParseStatus.FAILED.name());
                        record.setImportStatus(ImportStatus.FAILED);
                        record.setErrorMessage(result.getErrorMessage());
                        failCount++;
                        log.warn("action=PARSE_RECORD result=FAILED jobId={} row={} reason={}",
                                jobId, result.getRowNumber(), result.getErrorMessage());

                        ImportLog importLog = new ImportLog();
                        importLog.setTenantId(tenantId);
                        importLog.setUploadJobId(jobId);
                        importLog.setLogLevel("ERROR");
                        importLog.setMessage(result.getErrorMessage());
                        importLogRepository.save(importLog);
                    }

                    parsedRecordRepository.save(record);
                }

                job.setStatus(failCount == 0 ? "COMPLETED" : "COMPLETED_WITH_ERRORS");
                job.setTotalCount(results.size());
                job.setSuccessCount(successCount);
                job.setFailCount(failCount);
                job.setDupCount(dupCount);
                job.setFinishedAt(LocalDateTime.now());
                uploadJobRepository.save(job);

                log.info("action=PROCESS_IMPORT_JOB result=SUCCESS jobId={} total={} success={} fail={} dup={}",
                        jobId, results.size(), successCount, failCount, dupCount);
            });

        } catch (Exception e) {
            log.error("action=PROCESS_IMPORT_JOB result=FAILED jobId={} reason={}", jobId, e.getMessage(), e);
            importJobStatusService.markJobFailed(jobId, tenantId);
        }
    }

    private String calculateDedupKey(Long accountId, ParseResult result) {
        // SHA-256(accountId + date + amount + balance)
        // balance 為 null 時用空字串，確保信用卡類型仍可去重
        String raw = accountId + "|"
                + result.getTransactionDate() + "|"
                + result.getAmount().toPlainString() + "|"
                + (result.getBalance() != null ? result.getBalance().toPlainString() : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 標準，不可能不存在，但必須 catch checked exception
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String buildRawDataJson(ParseResult result) {
        List<String> raw = result.getRawData();
        if (raw == null || raw.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < raw.size(); i++) {
            sb.append("\"").append(raw.get(i).replace("\"", "\\\"")).append("\"");
            if (i < raw.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public JobStatusResponse getJobStatus(Long jobId, Long tenantId) {
        UploadJob job = uploadJobRepository.findById(jobId)
                .filter(j -> j.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCode.IMPORT_JOB_NOT_FOUND));

        return new JobStatusResponse(
                job.getId(),
                job.getStatus(),
                job.getParserKey(),
                job.getTotalCount(),
                job.getSuccessCount(),
                job.getFailCount(),
                job.getCreatedAt(),
                job.getFinishedAt()
        );
    }

    public List<ParsedRecordPreviewResponse> getJobPreview(Long jobId, Long tenantId) {
        return parsedRecordRepository
                .findByUploadJobIdAndTenantIdAndParseStatus(jobId, tenantId, "SUCCESS")
                .stream()
                .map(r -> new ParsedRecordPreviewResponse(
                        r.getId(),
                        r.getSourceRowNum(),
                        r.getTransactionDate(),
                        r.getDescription(),
                        r.getAmount(),
                        r.getBalance(),
                        r.getCurrencyCode(),
                        r.getImportStatus().name()
                ))
                .toList();
    }

    public List<ParsedRecordErrorResponse> getJobErrors(Long jobId, Long tenantId) {
        return parsedRecordRepository
                .findByUploadJobIdAndTenantIdAndParseStatus(jobId, tenantId, "FAILED")
                .stream()
                .map(r -> new ParsedRecordErrorResponse(
                        r.getId(),
                        r.getSourceRowNum(),
                        r.getRawData(),
                        r.getErrorMessage()
                ))
                .toList();
    }
}
