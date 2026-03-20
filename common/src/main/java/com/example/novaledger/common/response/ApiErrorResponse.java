package com.example.novaledger.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiErrorResponse {

    private String traceId;
    private String errorCode;
    private int status;
    private String message;
    private String path;
}
