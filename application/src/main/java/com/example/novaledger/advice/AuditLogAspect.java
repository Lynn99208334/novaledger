package com.example.novaledger.advice;

import com.example.novaledger.common.logging.AuditContext;
import com.example.novaledger.common.logging.AuditLog;
import com.example.novaledger.common.logging.AuditType;
import com.example.novaledger.common.security.AuthenticatedUserPrincipal;
import com.example.novaledger.finance.audit.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Service 層寫入操作 Audit Log。
 * 攔截標注 @AuditLog 的 Service method，根據 AuditType 決定 before/after_value：
 *
 *   CREATE：before = null，after = method 回傳值
 *   UPDATE：before = AuditContext 取得（Service 負責 set），after = method 回傳值
 *   DELETE：before = AuditContext 取得（Service 負責 set），after = null
 *
 * AuditContext 由 Service 在 proceed 前 set，Aspect 在 finally 負責 clear。
 *
 * 敏感欄位遮罩：直接使用 objectMapper.writeValueAsString() 序列化，
 * 讓 DTO 欄位上的 @Sensitive annotation 自動套用遮罩，不再手動處理 SENSITIVE_FIELDS。
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AuditLogAspect(AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(auditLog)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        AuditType type = auditLog.type();
        String action = auditLog.action();
        String targetType = resolveTargetType(joinPoint);

        Object result = null;
        Throwable thrown = null;

        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            thrown = ex;
        } finally {
            // 主 transaction 已結束（commit 或 rollback），此時寫 audit log 不會污染主 transaction
            try {
                Long userId = getCurrentUserId();
                Long tenantId = getCurrentTenantId();
                String ipAddress = getClientIp();

                String beforeValue = resolveBeforeValue(type);
                String afterValue = resolveAfterValue(type, result, thrown);
                String targetId = resolveTargetId(type, result);

                auditLogService.save(tenantId, userId, action, targetType,
                        targetId, beforeValue, afterValue, ipAddress);
            } catch (Exception e) {
                log.error("action=AUDIT_LOG_FAILED reason={}", e.getMessage(), e);
            } finally {
                AuditContext.clear();
            }
        }

        if (thrown != null) throw thrown;
        return result;
    }

    // ── before / after 邏輯 ──────────────────────────────────

    private String resolveBeforeValue(AuditType type) {
        return switch (type) {
            case CREATE -> null;
            case UPDATE, DELETE -> AuditContext.getBeforeValue();
        };
    }

    private String resolveAfterValue(AuditType type, Object result, Throwable thrown) {
        if (thrown != null) return null;
        return switch (type) {
            case CREATE, UPDATE -> serializeResult(result);
            case DELETE -> null;
        };
    }

    private String resolveTargetId(AuditType type, Object result) {
        if (type == AuditType.DELETE || result == null) return null;
        try {
            var method = result.getClass().getMethod("getId");
            Object id = method.invoke(result);
            return id != null ? id.toString() : null;
        } catch (Exception ignored) {}
        return null;
    }

    // ── 輔助方法 ────────────────────────────────────────────

    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
                return principal.getUserId();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Long getCurrentTenantId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
                return principal.getTenantId();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String resolveTargetType(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        if (className.endsWith("Service")) {
            return className.substring(0, className.length() - "Service".length());
        }
        return className;
    }

    /**
     * 直接用 objectMapper.writeValueAsString() 序列化，
     * 讓 DTO 欄位上的 @Sensitive annotation 自動套用遮罩。
     */
    private String serializeResult(Object result) {
        if (result == null) return null;
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("action=AUDIT_SERIALIZE_RESULT result=FAILED reason={}", e.getMessage());
            return null;
        }
    }
}
