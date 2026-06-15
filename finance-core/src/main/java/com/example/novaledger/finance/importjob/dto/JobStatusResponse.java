package com.example.novaledger.finance.importjob.dto;
import java.time.LocalDateTime;

public class JobStatusResponse {
    private Long jobId;
    private String status;
    private String parserKey;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String failReason;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;

    public JobStatusResponse(Long jobId, String status, String parserKey,
                             Integer totalCount, Integer successCount, Integer failCount,
                             String failReason,
                             LocalDateTime createdAt, LocalDateTime finishedAt) {
        this.jobId = jobId;
        this.status = status;
        this.parserKey = parserKey;
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.failCount = failCount;
        this.failReason = failReason;
        this.createdAt = createdAt;
        this.finishedAt = finishedAt;
    }

    public Long getJobId() { return jobId; }
    public String getStatus() { return status; }
    public String getParserKey() { return parserKey; }
    public Integer getTotalCount() { return totalCount; }
    public Integer getSuccessCount() { return successCount; }
    public Integer getFailCount() { return failCount; }
    public String getFailReason() { return failReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
}
