package com.example.novaledger.advice;

import com.example.novaledger.common.logging.AuditAction;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Controller 層 HTTP 進出 log。
 * 格式：action=XXX method=XXX controller=XXX [result=XXX] [reason=XXX]
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Before("within(@org.springframework.web.bind.annotation.RestController *)")
    public void logRequest(JoinPoint joinPoint) {
        log.info("action={} method={} controller={}",
                AuditAction.HTTP_REQUEST,
                joinPoint.getSignature().getName(),
                joinPoint.getSignature().getDeclaringType().getSimpleName());
    }

    @AfterReturning(pointcut = "within(@org.springframework.web.bind.annotation.RestController *)",
            returning = "result")
    public void logResponse(JoinPoint joinPoint, Object result) {
        log.info("action={} method={} controller={} result=SUCCESS",
                AuditAction.HTTP_RESPONSE,
                joinPoint.getSignature().getName(),
                joinPoint.getSignature().getDeclaringType().getSimpleName());
    }

    @AfterThrowing(
            pointcut = "within(@org.springframework.web.bind.annotation.RestController *)",
            throwing = "ex")
    public void logException(JoinPoint joinPoint, Exception ex) {
        log.error("action={} method={} controller={} result=FAILED reason={}",
                AuditAction.HTTP_ERROR,
                joinPoint.getSignature().getName(),
                joinPoint.getSignature().getDeclaringType().getSimpleName(),
                ex.getClass().getSimpleName());
    }
}
