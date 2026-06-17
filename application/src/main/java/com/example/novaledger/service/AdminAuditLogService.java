package com.example.novaledger.service;

import com.example.novaledger.auth.repository.UserRepository;
import com.example.novaledger.dto.AuditLogDto;
import com.example.novaledger.finance.audit.repository.AuditLogRepository;
import com.example.novaledger.util.UserQueryHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminAuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final UserQueryHelper userQueryHelper;

    public AdminAuditLogService(AuditLogRepository auditLogRepository,
                                UserRepository userRepository,
                                UserQueryHelper userQueryHelper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.userQueryHelper = userQueryHelper;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getLogs(String username, String action,
                                     LocalDate dateFrom, LocalDate dateTo,
                                     int page, int size) {
        LocalDateTime from = (dateFrom != null) ? dateFrom.atStartOfDay() : null;
        LocalDateTime to = (dateTo != null) ? dateTo.plusDays(1).atStartOfDay().minusNanos(1) : null;
        Pageable pageable = PageRequest.of(page, size);

        List<Long> userIds = userQueryHelper.resolveUserIds(username);
        if (userIds != null && userIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return auditLogRepository
                .searchLogs(userIds, action, from, to, pageable)
                .map(entity -> {
                    AuditLogDto dto = AuditLogDto.from(entity);
                    if (entity.getUserId() != null) {
                        userRepository.findById(entity.getUserId())
                                .ifPresent(u -> dto.setUsername(u.getUsername()));
                    }
                    return dto;
                });
    }
}
