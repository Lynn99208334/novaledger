package com.example.novaledger.finance.importrecord.entity;

import com.example.novaledger.common.entity.BaseTenantEntity;
import com.example.novaledger.finance.enums.ImportStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "parsed_records")
public class ParsedRecord extends BaseTenantEntity {

    @Column(name = "upload_job_id", nullable = false)
    private Long uploadJobId;

    @Column(name = "upload_file_id", nullable = false)
    private Long uploadFileId;

    @Column(name = "source_row_num", nullable = false)
    private Integer sourceRowNum;

    @Column(name = "raw_data", nullable = false, columnDefinition = "JSON")
    private String rawData;

    @Column(name = "parse_status", nullable = false, length = 30)
    private String parseStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_status", nullable = false, length = 20)
    private ImportStatus importStatus = ImportStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance", precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "currency_code", length = 10)
    private String currencyCode = "TWD";

    @Column(name = "dedup_key", length = 64, unique = true)
    private String dedupKey;

    public ParsedRecord() {}

    public Long getUploadJobId() { return uploadJobId; }
    public void setUploadJobId(Long uploadJobId) { this.uploadJobId = uploadJobId; }

    public Long getUploadFileId() { return uploadFileId; }
    public void setUploadFileId(Long uploadFileId) { this.uploadFileId = uploadFileId; }

    public Integer getSourceRowNum() { return sourceRowNum; }
    public void setSourceRowNum(Integer sourceRowNum) { this.sourceRowNum = sourceRowNum; }

    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }

    public String getParseStatus() { return parseStatus; }
    public void setParseStatus(String parseStatus) { this.parseStatus = parseStatus; }

    public ImportStatus getImportStatus() { return importStatus; }
    public void setImportStatus(ImportStatus importStatus) { this.importStatus = importStatus; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }
}