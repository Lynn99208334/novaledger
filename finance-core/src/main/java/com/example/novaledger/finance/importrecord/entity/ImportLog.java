package com.example.novaledger.finance.importrecord.entity;

import com.example.novaledger.common.entity.BaseTenantEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "import_logs")
public class ImportLog extends BaseTenantEntity {

    @Column(name = "upload_job_id", nullable = false)
    private Long uploadJobId;

    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "log_level", nullable = false, length = 20)
    private String logLevel;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    public ImportLog() {}

    public Long getUploadJobId() { return uploadJobId; }
    public void setUploadJobId(Long uploadJobId) { this.uploadJobId = uploadJobId; }

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}