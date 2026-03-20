package com.example.novaledger.common.response;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ApiErrorResponse error;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.data = data;
        return r;
    }

    public static ApiResponse<Void> ok() {
        ApiResponse<Void> r = new ApiResponse<>();
        r.success = true;
        return r;
    }

    public static ApiResponse<Void> fail(ApiErrorResponse error) {
        ApiResponse<Void> r = new ApiResponse<>();
        r.success = false;
        r.error = error;
        return r;
    }
}

