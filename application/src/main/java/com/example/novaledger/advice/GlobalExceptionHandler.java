package com.example.novaledger.advice;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.common.response.ApiErrorResponse;
import com.example.novaledger.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ApiErrorResponse buildError(
            HttpServletRequest req,
            ErrorCode code,
            HttpStatus status,
            String message
    ) {
        String traceId = (String) req.getAttribute("traceId");
        return new ApiErrorResponse(
                traceId,
                code.getCode(),
                status.value(),
                message != null ? message : code.getMessage(),
                req.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest req
    ) {
        StringBuilder sb = new StringBuilder();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(err ->
                        sb.append(err.getField())
                                .append(": ")
                                .append(err.getDefaultMessage())
                                .append("; ")
                );

        log.warn("action=VALIDATION_ERROR uri={} reason={}", req.getRequestURI(), sb);

        ApiErrorResponse error = buildError(req, ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, sb.toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(error));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(
            BusinessException ex,
            HttpServletRequest req
    ) {
        ErrorCode code = ex.getErrorCode();
        log.warn("action=BUSINESS_ERROR uri={} errorCode={} reason={}", req.getRequestURI(), code.getCode(), ex.getMessage());

        ApiErrorResponse error = buildError(req, code, code.getHttpStatusEnum(), ex.getMessage());
        return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.fail(error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest req
    ) {
        log.warn("action=ILLEGAL_ARGUMENT uri={} reason={}", req.getRequestURI(), ex.getMessage());

        ApiErrorResponse error = buildError(req, ErrorCode.BUSINESS_ERROR, HttpStatus.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception ex,
            HttpServletRequest req
    ) {
        log.error("action=UNEXPECTED_ERROR uri={} reason={}", req.getRequestURI(), ex.getMessage(), ex);

        ApiErrorResponse error = buildError(req, ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(error));
    }
}
