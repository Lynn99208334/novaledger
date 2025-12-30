package com.example.tableaudemov2.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // ========================
    // 通用 / 系統層
    // ========================
    VALIDATION_ERROR("E40001", "Validation Failed", HttpStatus.BAD_REQUEST),
    BUSINESS_ERROR("E40002", "Business Error", HttpStatus.BAD_REQUEST),

    ACCESS_DENIED("E40301", "Access Denied", HttpStatus.FORBIDDEN),
    NOT_FOUND("E40401", "Resource Not Found", HttpStatus.NOT_FOUND),

    INTERNAL_ERROR("E50001", "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR),

    // ========================
    // JWT / Security
    // ========================
    JWT_EXPIRED("E40101", "JWT Token Expired", HttpStatus.UNAUTHORIZED),
    JWT_INVALID("E40102", "Invalid JWT Token", HttpStatus.UNAUTHORIZED),

    // ========================
    // Auth / Account
    // ========================
    EMAIL_NOT_VERIFIED("AUTH_001", "Email Not Verified", HttpStatus.UNAUTHORIZED),
    EMAIL_TOKEN_INVALID("AUTH_002", "Invalid Email Verification Token", HttpStatus.BAD_REQUEST),
    EMAIL_TOKEN_EXPIRED("AUTH_003", "Email Verification Token Expired", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_VERIFIED("AUTH_004", "Email Already Verified", HttpStatus.BAD_REQUEST),
    EMAIL_RESEND_TOO_FREQUENT("AUTH_005", "Verification email sent too frequently", HttpStatus.TOO_MANY_REQUESTS),
    USER_NOT_FOUND("AUTH_006", "User Not Found", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS("AUTH_007", "Username Already Exists", HttpStatus.CONFLICT),

    // ===== Email Verification =====
    EMAIL_ALREADY_EXISTS("AUTH_011", "Email Already Exists", HttpStatus.CONFLICT),
    EMAIL_VERIFY_TOKEN_INVALID("AUTH_012", "Email Verification Token Invalid", HttpStatus.BAD_REQUEST),
    EMAIL_VERIFY_TOKEN_EXPIRED("AUTH_013", "Email Verification Token Expired", HttpStatus.BAD_REQUEST),

    ACCOUNT_DISABLED("AUTH_020", "Account Disabled", HttpStatus.FORBIDDEN),
    ACCOUNT_NOT_ACTIVE("AUTH_021", "Account Not Active", HttpStatus.FORBIDDEN),

    LOGIN_FAILED("AUTH_030", "Login Failed", HttpStatus.UNAUTHORIZED),
    LOGOUT_FAILED("AUTH_031", "Logout Failed", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
