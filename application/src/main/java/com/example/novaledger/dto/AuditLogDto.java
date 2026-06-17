package com.example.novaledger.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogDto {

    private Long id;
    private Long tenantId;
    private Long userId;
    private String username;
    private String action;
    private String targetType;
    private String targetId;
    private String beforeValue;
    private String afterValue;
    private String ipAddress;
    private LocalDateTime createdAt;

    public static AuditLogDto from(com.example.novaledger.finance.audit.entity.AuditLog entity) {
        return AuditLogDto.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .userId(entity.getUserId())
                .action(entity.getAction())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .beforeValue(entity.getBeforeValue())
                .afterValue(entity.getAfterValue())
                .ipAddress(entity.getIpAddress())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
