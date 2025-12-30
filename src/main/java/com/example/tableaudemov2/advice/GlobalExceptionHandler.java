package com.example.tableaudemov2.advice;

import com.example.tableaudemov2.enums.ErrorCode;
import com.example.tableaudemov2.exception.ApiErrorResponse;
import com.example.tableaudemov2.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ApiErrorResponse buildError(HttpServletRequest req, ErrorCode code, HttpStatus status, String message) {
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
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest req
    ) {

        StringBuilder sb = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                sb.append(err.getField()).append(": ").append(err.getDefaultMessage()).append("; ")
        );

        ApiErrorResponse error = buildError(
                req,
                ErrorCode.VALIDATION_ERROR,
                HttpStatus.BAD_REQUEST,
                sb.toString()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(
            BusinessException ex,
            HttpServletRequest req
    ) {
        ApiErrorResponse error = buildError(
                req,
                ex.getErrorCode(),   // ⭐ 關鍵
                ex.getStatus(),
                ex.getMessage()
        );
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex, HttpServletRequest req) {
        ApiErrorResponse error = buildError(req, ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest req
    ) {
        ApiErrorResponse error = buildError(
                req,
                ErrorCode.BUSINESS_ERROR,
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        return ResponseEntity.badRequest().body(error);
    }


}
