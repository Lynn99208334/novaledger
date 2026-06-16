package com.example.novaledger.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParserOverviewDto {

    private String bankCode;
    private String bankName;
    private String fileType;
    private String parserKey;
    private boolean supportsAutoDetect;
    private boolean supportsAccountExtraction;
}
