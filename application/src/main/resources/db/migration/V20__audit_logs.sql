-- ========================================
-- NovaLedger Schema
-- Flyway Migration V20
-- Audit Logs
-- ========================================

CREATE TABLE audit_logs
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id    BIGINT       NULL,
    user_id      BIGINT       NULL,
    action       VARCHAR(100) NOT NULL,
    target_type  VARCHAR(100) NOT NULL,
    target_id    VARCHAR(100) NULL,
    before_value JSON         NULL,
    after_value  JSON         NULL,
    ip_address   VARCHAR(45)  NULL,
    created_at   DATETIME     NOT NULL,
    updated_at   DATETIME     NULL,
    INDEX idx_audit_tenant_id (tenant_id),
    INDEX idx_audit_user_id (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
