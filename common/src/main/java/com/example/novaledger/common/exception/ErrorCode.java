package com.example.novaledger.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // ========================
    // 通用 / 系統層
    // ========================
    VALIDATION_ERROR("E40001", "Validation Failed", 400),
    BUSINESS_ERROR("E40002", "Business Error", 400),

    ACCESS_DENIED("E40301", "Access Denied", 403),
    NOT_FOUND("E40401", "Resource Not Found", 404),

    INTERNAL_ERROR("E50001", "Internal Server Error", 500),

    // ========================
    // JWT / Security
    // ========================
    JWT_EXPIRED("E40101", "JWT Token Expired", 401),
    JWT_INVALID("E40102", "Invalid JWT Token", 401),

    // ========================
    // Auth
    // ========================
    EMAIL_NOT_VERIFIED("AUTH_001", "Email Not Verified", 401),
    EMAIL_TOKEN_INVALID("AUTH_002", "Invalid Email Verification Token", 400),
    EMAIL_TOKEN_EXPIRED("AUTH_003", "Email Verification Token Expired", 400),
    EMAIL_ALREADY_VERIFIED("AUTH_004", "Email Already Verified", 400),
    EMAIL_RESEND_TOO_FREQUENT("AUTH_005", "Verification email sent too frequently", 429),
    USER_NOT_FOUND("AUTH_006", "User Not Found", 404),
    USERNAME_ALREADY_EXISTS("AUTH_007", "Username Already Exists", 409),

    EMAIL_ALREADY_EXISTS("AUTH_011", "Email Already Exists", 409),
    EMAIL_VERIFY_TOKEN_INVALID("AUTH_012", "Email Verification Token Invalid", 400),
    EMAIL_VERIFY_TOKEN_EXPIRED("AUTH_013", "Email Verification Token Expired", 400),

    ACCOUNT_DISABLED("AUTH_020", "Account Disabled", 403),
    ACCOUNT_NOT_ACTIVE("AUTH_021", "Account Not Active", 403),

    LOGIN_FAILED("AUTH_030", "Login Failed", 401),
    PASSWORD_INCORRECT("AUTH_032", "Password Incorrect", 401),
    LOGOUT_FAILED("AUTH_031", "Logout Failed", 400),

    // ========================
    // Account
    // ========================
    ACCOUNT_001("ACCOUNT_001", "Account Not Found", 404),

    // ========================
    // Credit Card
    // ========================
    CARD_001("CARD_001", "Credit Card Not Found", 404),

    // ========================
    // Import
    // ========================
    FILE_EMPTY("IMPORT_001", "File is empty", 400),
    FILE_TOO_LARGE("IMPORT_002", "File size exceeds 10MB limit", 400),
    FILE_INVALID("IMPORT_003", "Invalid file name", 400),
    FILE_TYPE_NOT_SUPPORTED("IMPORT_004", "Only .xlsx, .xls or .csv files are supported", 400),
    IMPORT_PARSER_NOT_FOUND("IMPORT_005", "Parser not found for the given format", 400),
    IMPORT_FILE_READ_FAILED("IMPORT_006", "Failed to read uploaded file", 500),
    IMPORT_JOB_NOT_FOUND("IMPORT_007", "Import job not found", 404),
    IMPORT_HTML_PARSE_FAILED("IMPORT_008", "Failed to parse HTML-based XLS file", 500),

    // ========================
    // Transaction
    // ========================
    TRANSACTION_NOT_FOUND("TX_001", "Transaction Not Found", 404),
    TRANSACTION_SOURCE_INVALID("TX_002", "Transaction must have either accountId or creditCardId", 400),

    // ========================
    // Bank
    // ========================
    BANK_NOT_FOUND("BANK_001", "Bank Not Found", 404),
    BANK_CODE_DUPLICATE("BANK_002", "Bank code already exists", 409),

    // ========================
    // Exchange Rate
    // ========================
    EXCHANGE_RATE_NOT_FOUND("EXCHANGE_001", "Exchange Rate Not Found", 404),

    ;

    private final String code;
    private final String message;
    private final int httpStatus;


    ErrorCode(String code, String message, int httpStatus) {
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

    public int getHttpStatus() {
        return httpStatus;
    }

    public HttpStatus getHttpStatusEnum() {
        return HttpStatus.valueOf(this.httpStatus);
    }
}
