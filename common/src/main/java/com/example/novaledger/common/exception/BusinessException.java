package com.example.novaledger.common.exception;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 最常用建構子
     * Domain 只關心「發生什麼錯」
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 允許覆寫訊息（少用）
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
