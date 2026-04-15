package com.example.novaledger.finance.importjob.entity;
import com.example.novaledger.common.entity.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "upload_jobs")
public class UploadJob extends BaseTenantEntity {

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "total_count")
    private Integer totalCount;

    @Column(name = "success_count")
    private Integer successCount;

    @Column(name = "fail_count")
    private Integer failCount;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "detected_account_number", length = 50)
    private String detectedAccountNumber;

    @Column(name = "detected_format_id")
    private Long detectedFormatId;

    @Column(name = "parser_key", length = 100)
    private String parserKey;

    public UploadJob() {}

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }

    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }

    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public String getDetectedAccountNumber() { return detectedAccountNumber; }
    public void setDetectedAccountNumber(String detectedAccountNumber) { this.detectedAccountNumber = detectedAccountNumber; }

    public Long getDetectedFormatId() { return detectedFormatId; }
    public void setDetectedFormatId(Long detectedFormatId) { this.detectedFormatId = detectedFormatId; }

    public String getParserKey() {
        return parserKey;
    }

    public void setParserKey(String parserKey) {
        this.parserKey = parserKey;
    }
}