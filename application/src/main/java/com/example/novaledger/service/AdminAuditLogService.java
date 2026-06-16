package com.example.novaledger.service;

import com.example.novaledger.dto.AuditLogDto;
import com.example.novaledger.finance.audit.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AdminAuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AdminAuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getLogs(Long userId, String action,
                                     LocalDate dateFrom, LocalDate dateTo,
                                     int page, int size) {
        LocalDateTime from = (dateFrom != null) ? dateFrom.atStartOfDay() : null;
        // dateTo 包含當天最後一刻
        LocalDateTime to = (dateTo != null) ? dateTo.plusDays(1).atStartOfDay().minusNanos(1) : null;

        return auditLogRepository
                .searchLogs(userId, action, from, to, PageRequest.of(page, size))
                .map(AuditLogDto::from);
    }
}
