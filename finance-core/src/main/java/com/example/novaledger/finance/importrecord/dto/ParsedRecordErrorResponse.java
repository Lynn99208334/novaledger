package com.example.novaledger.finance.importrecord.dto;

public class ParsedRecordErrorResponse {
    private Long recordId;
    private Integer sourceRowNum;
    private String rawData;
    private String errorMessage;

    public ParsedRecordErrorResponse(Long recordId, Integer sourceRowNum,
                                     String rawData, String errorMessage) {
        this.recordId = recordId;
        this.sourceRowNum = sourceRowNum;
        this.rawData = rawData;
        this.errorMessage = errorMessage;
    }

    public Long getRecordId() { return recordId; }
    public Integer getSourceRowNum() { return sourceRowNum; }
    public String getRawData() { return rawData; }
    public String getErrorMessage() { return errorMessage; }
}