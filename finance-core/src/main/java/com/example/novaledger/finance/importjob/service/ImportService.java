package com.example.novaledger.finance.importjob.service;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.finance.enums.ImportStatus;
import com.example.novaledger.finance.enums.ParseStatus;
import com.example.novaledger.finance.importjob.dto.JobStatusResponse;
import com.example.novaledger.finance.importjob.dto.UploadJobResponse;
import com.example.novaledger.finance.importjob.entity.UploadFile;
import com.example.novaledger.finance.importjob.entity.UploadJob;
import com.example.novaledger.finance.importjob.parser.ParseResult;
import com.example.novaledger.finance.importjob.parser.ParserRegistry;
import com.example.novaledger.finance.importjob.repository.UploadFileRepository;
import com.example.novaledger.finance.importjob.repository.UploadJobRepository;
import com.example.novaledger.finance.importrecord.dto.ParsedRecordErrorResponse;
import com.example.novaledger.finance.importrecord.dto.ParsedRecordPreviewResponse;
import com.example.novaledger.finance.importrecord.entity.ImportLog;
import com.example.novaledger.finance.importrecord.entity.ParsedRecord;
import com.example.novaledger.finance.importrecord.repository.ImportLogRepository;
import com.example.novaledger.finance.importrecord.repository.ParsedRecordRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ImportService {

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

    public ImportService(UploadJobRepository uploadJobRepository,
                         UploadFileRepository uploadFileRepository,
                         FileParserService fileParserService,
                         ParsedRecordRepository parsedRecordRepository,
                         ImportLogRepository importLogRepository,
                         ParserRegistry parserRegistry,
                         ImportJobStatusService importJobStatusService,
                         TransactionTemplate transactionTemplate) {
        this.uploadJobRepository = uploadJobRepository;
        this.uploadFileRepository = uploadFileRepository;
        this.fileParserService = fileParserService;
        this.parsedRecordRepository = parsedRecordRepository;
        this.importLogRepository = importLogRepository;
        this.parserRegistry = parserRegistry;
        this.importJobStatusService = importJobStatusService;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public UploadJobResponse createUploadJob(MultipartFile file, String jobType,
                                             String bankCode, Long tenantId, Long userId) {
        validateFile(file);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMPORT_FILE_READ_FAILED);
        }

        String parserKey = parserRegistry.resolveParserKey(bankCode, file.getOriginalFilename());

        UploadJob job = new UploadJob();
        job.setTenantId(tenantId);
        job.setStatus("PENDING");
        job.setCreatedBy(userId);
        job.setJobType(jobType);
        job.setParserKey(parserKey);
        uploadJobRepository.save(job);

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
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }
    }

    @Async
    public void processImportJob(Long jobId, Long tenantId, byte[] fileBytes,
                                 String originalFilename, String mimeType, long fileSize) {
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

                for (ParseResult result : results) {
                    ParsedRecord record = new ParsedRecord();
                    record.setTenantId(tenantId);
                    record.setUploadJobId(jobId);
                    record.setUploadFileId(uploadFile.getId());
                    record.setSourceRowNum(result.getRowNumber());
                    record.setRawData(buildRawDataJson(result));

                    if (result.isSuccess()) {
                        record.setParseStatus(ParseStatus.SUCCESS.name());
                        record.setImportStatus(ImportStatus.PENDING);
                        record.setTransactionDate(result.getTransactionDate());
                        record.setDescription(result.getDescription());
                        record.setAmount(result.getAmount());
                        record.setBalance(result.getBalance());
                        record.setCurrencyCode("TWD");
                        successCount++;
                    } else {
                        record.setParseStatus(ParseStatus.FAILED.name());
                        record.setImportStatus(ImportStatus.FAILED);
                        record.setErrorMessage(result.getErrorMessage());
                        failCount++;

                        ImportLog log = new ImportLog();
                        log.setTenantId(tenantId);
                        log.setUploadJobId(jobId);
                        log.setLogLevel("ERROR");
                        log.setMessage(result.getErrorMessage());
                        importLogRepository.save(log);
                    }

                    parsedRecordRepository.save(record);
                }

                job.setStatus(failCount == 0 ? "COMPLETED" : "COMPLETED_WITH_ERRORS");
                job.setTotalCount(results.size());
                job.setSuccessCount(successCount);
                job.setFailCount(failCount);
                job.setFinishedAt(LocalDateTime.now());
                uploadJobRepository.save(job);
            });

        } catch (Exception e) {
            importJobStatusService.markJobFailed(jobId, tenantId);
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