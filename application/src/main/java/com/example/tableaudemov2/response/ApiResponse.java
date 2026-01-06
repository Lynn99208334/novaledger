package com.example.tableaudemov2.response;

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

    public static ApiResponse<?> fail(ApiErrorResponse error) {
        ApiResponse<?> r = new ApiResponse<>();
        r.success = false;
        r.error = error;
        return r;
    }
}

