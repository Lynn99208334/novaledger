package com.example.novaledger.finance.audit.service;

import com.example.novaledger.finance.audit.entity.AuditLog;
import com.example.novaledger.finance.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // REQUIRES_NEW：audit log 寫入失敗不應 rollback 主業務
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Long tenantId, Long userId, String action, String targetType,
                     String targetId, String beforeValue, String afterValue, String ipAddress) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setTenantId(tenantId);
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setTargetType(targetType);
            auditLog.setTargetId(targetId);
            auditLog.setBeforeValue(beforeValue);
            auditLog.setAfterValue(afterValue);
            auditLog.setIpAddress(ipAddress);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("action=SAVE_AUDIT_LOG result=FAILED reason={}", e.getMessage(), e);
        }
    }
}
