package com.example.novaledger.advice;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Before("within(@org.springframework.web.bind.annotation.RestController *)")
    public void logRequest(JoinPoint joinPoint) {
        log.info("➡️ Request: {}.{} args={}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                joinPoint.getArgs());
    }

    @AfterReturning(pointcut = "within(@org.springframework.web.bind.annotation.RestController *)",
            returning = "result")
    public void logResponse(Object result) {
        log.info("⬅️ Response: {}", result);
    }

    @AfterThrowing(pointcut = "within(@org.springframework.web.bind.annotation.RestController *)",
            throwing = "ex")
    public void logException(Exception ex) {
        log.error("❗ Exception: {}", ex.getMessage(), ex);
    }
}
