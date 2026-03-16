-- ========================================
-- NovaLedger Schema
-- Flyway Migration V1
-- Users & Tenants
-- ========================================

SET NAMES utf8mb4;SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE USERS (
    ID                        BIGINT       AUTO_INCREMENT PRIMARY KEY,
    USERNAME                  VARCHAR(100) NOT NULL,
    EMAIL                     VARCHAR(255) NOT NULL,
    PASSWORD                  VARCHAR(255) NOT NULL,
    ENABLED                   BOOLEAN      NOT NULL,
    STATUS                    VARCHAR(20)  NOT NULL,
    EMAIL_VERIFIED            BOOLEAN      NOT NULL,
    VERIFIED_AT               DATETIME,
    EMAIL_VERIFY_TOKEN        VARCHAR(100),
    EMAIL_VERIFY_EXPIRED_AT   DATETIME,
    EMAIL_VERIFY_LAST_SENT_AT DATETIME,
    IS_SYSTEM_ADMIN           BOOLEAN      NOT NULL,
    LAST_LOGIN_AT             DATETIME,
    CREATED_AT                DATETIME     NOT NULL,
    UPDATED_AT                DATETIME     NOT NULL,
    DELETED_AT                DATETIME,
    UNIQUE KEY uk_username (USERNAME),
    UNIQUE KEY uk_email (EMAIL),
    INDEX idx_status_deleted (STATUS, DELETED_AT),
    INDEX idx_email_verified (EMAIL_VERIFIED),
    INDEX idx_email_verify_token (EMAIL_VERIFY_TOKEN)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE TENANTS (
    ID            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    CODE          VARCHAR(100) NOT NULL,
    NAME          VARCHAR(255) NOT NULL,
    TYPE          VARCHAR(20)  NOT NULL,
    PLAN          VARCHAR(20)  NOT NULL,
    OWNER_USER_ID BIGINT       NOT NULL,
    STATUS        VARCHAR(20)  NOT NULL,
    CREATED_AT    DATETIME     NOT NULL,
    UPDATED_AT    DATETIME     NOT NULL,
    DELETED_AT    DATETIME,
    UNIQUE KEY uk_tenant_code (CODE),
    INDEX idx_tenant_owner (OWNER_USER_ID),
    INDEX idx_status_deleted (STATUS, DELETED_AT),
    INDEX idx_deleted_at (DELETED_AT),
    CONSTRAINT fk_tenant_owner
        FOREIGN KEY (OWNER_USER_ID)
            REFERENCES USERS(ID)
            ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
