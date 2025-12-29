package com.example.tableaudemov2.enums;

public enum ErrorCode {

    // ========================
    // 通用 / 系統層
    // ========================
    VALIDATION_ERROR("E40001", "Validation Failed"),
    BUSINESS_ERROR("E40002", "Business Error"),

    ACCESS_DENIED("E40301", "Access Denied"),
    NOT_FOUND("E40401", "Resource Not Found"),

    INTERNAL_ERROR("E50001", "Internal Server Error"),

    // ========================
    // JWT / Security
    // ========================
    JWT_EXPIRED("E40101", "JWT Token Expired"),
    JWT_INVALID("E40102", "Invalid JWT Token"),

    // ========================
    // Auth / Account（新增）
    // ========================
    EMAIL_NOT_VERIFIED("AUTH_001", "Email Not Verified"),
    EMAIL_TOKEN_INVALID("AUTH_002", "Invalid Email Verification Token"),
    EMAIL_TOKEN_EXPIRED("AUTH_003", "Email Verification Token Expired"),
    EMAIL_ALREADY_VERIFIED("AUTH_004", "Email Already Verified"),

    EMAIL_ALREADY_EXISTS("AUTH_011", "Email Already Exists"),
    USERNAME_ALREADY_EXISTS("AUTH_012", "Username Already Exists"),
    USER_NOT_FOUND("AUTH_013", "User Not Found"),

    ACCOUNT_DISABLED("AUTH_020", "Account Disabled"),
    ACCOUNT_NOT_ACTIVE("AUTH_021", "Account Not Active"),

    LOGIN_FAILED("AUTH_030", "Login Failed"),
    LOGOUT_FAILED("AUTH_031", "Logout Failed");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
