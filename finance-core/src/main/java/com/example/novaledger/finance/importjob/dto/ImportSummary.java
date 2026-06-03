package com.example.novaledger.finance.importjob.dto;

public class ImportSummary {

    private Long jobId;
    private int importedCount;

    public ImportSummary(Long jobId, int importedCount) {
        this.jobId = jobId;
        this.importedCount = importedCount;
    }

    public Long getId() { return jobId; }
    public Long getJobId() { return jobId; }
    public int getImportedCount() { return importedCount; }
}
