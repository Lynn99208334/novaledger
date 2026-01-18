package com.example.tableaudemov2.controller;

import com.example.tableaudemov2.common.exception.BusinessException;
import com.example.tableaudemov2.common.exception.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestExceptionController {
    @GetMapping("/test/business-exception")
    public void businessException() {
        System.out.println(">>> Controller HIT");
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    @GetMapping("/test/runtime-exception")
    public void runtimeException() {
        throw new RuntimeException("boom");
    }
}
