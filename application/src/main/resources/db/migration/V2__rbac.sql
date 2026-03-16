-- ========================================
-- NovaLedger Schema
-- Flyway Migration V2
-- RBAC：PERMISSIONS、ROLES、ROLE_PERMISSIONS、USER_TENANTS
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE PERMISSIONS (
    ID          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    CODE        VARCHAR(150) NOT NULL,
    NAME        VARCHAR(150) NOT NULL,
    RESOURCE    VARCHAR(100) NOT NULL,
    ACTION      VARCHAR(50)  NOT NULL,
    DESCRIPTION VARCHAR(255) NOT NULL,
    CREATED_AT  DATETIME     NOT NULL,
    UPDATED_AT  DATETIME     NOT NULL,
    UNIQUE KEY uk_permission_code  (CODE),
    UNIQUE KEY uk_resource_action  (RESOURCE, ACTION),
    INDEX idx_permission_resource  (RESOURCE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ROLES (
    ID             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    CODE           VARCHAR(100) NOT NULL,
    NAME           VARCHAR(100) NOT NULL,
    DESCRIPTION    VARCHAR(255),
    TENANT_ID      BIGINT,
    IS_SYSTEM_ROLE BOOLEAN      NOT NULL,
    CREATED_AT     DATETIME     NOT NULL,
    UPDATED_AT     DATETIME     NOT NULL,
    DELETED_AT     DATETIME,
    UNIQUE KEY uk_role_code_tenant  (CODE, TENANT_ID),
    INDEX idx_tenant_deleted        (TENANT_ID, DELETED_AT),
    INDEX idx_role_system           (IS_SYSTEM_ROLE),
    CONSTRAINT fk_role_tenant
        FOREIGN KEY (TENANT_ID)
            REFERENCES TENANTS(ID)
            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ROLES (CODE, NAME, DESCRIPTION, TENANT_ID, IS_SYSTEM_ROLE, CREATED_AT, UPDATED_AT)
VALUES
    ('OWNER',  '擁有者', '租戶擁有者，具所有操作權限',   NULL, TRUE, NOW(), NOW()),
    ('ADMIN',  '管理員', '租戶管理員，可管理成員與設定', NULL, TRUE, NOW(), NOW()),
    ('MEMBER', '成員',   '一般成員，可記帳與查看',       NULL, TRUE, NOW(), NOW());

CREATE TABLE ROLE_PERMISSIONS (
    ROLE_ID       BIGINT   NOT NULL,
    PERMISSION_ID BIGINT   NOT NULL,
    CREATED_AT    DATETIME NOT NULL,
    PRIMARY KEY (ROLE_ID, PERMISSION_ID),
    INDEX idx_permission_role  (PERMISSION_ID),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (ROLE_ID)
            REFERENCES ROLES(ID)
            ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (PERMISSION_ID)
            REFERENCES PERMISSIONS(ID)
            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE USER_TENANTS (
    ID                 BIGINT      AUTO_INCREMENT PRIMARY KEY,
    USER_ID            BIGINT      NOT NULL,
    TENANT_ID          BIGINT      NOT NULL,
    ROLE_ID            BIGINT      NOT NULL,
    STATUS             VARCHAR(20) NOT NULL,
    JOINED_AT          DATETIME    NOT NULL,
    REMOVED_BY_USER_ID BIGINT,
    DELETED_AT         DATETIME,
    ACTIVE_KEY         TINYINT GENERATED ALWAYS AS (
                           CASE WHEN STATUS = 'ACTIVE' AND DELETED_AT IS NULL
                               THEN 1 ELSE NULL END
                       ) STORED,
    UNIQUE KEY uk_user_tenant_active  (USER_ID, TENANT_ID, ACTIVE_KEY),
    INDEX idx_user_tenant_status      (USER_ID, TENANT_ID, STATUS),
    INDEX idx_tenant_role             (TENANT_ID, ROLE_ID),
    CONSTRAINT fk_user_tenants_user
        FOREIGN KEY (USER_ID)
            REFERENCES USERS(ID)
            ON DELETE CASCADE,
    CONSTRAINT fk_user_tenants_tenant
        FOREIGN KEY (TENANT_ID)
            REFERENCES TENANTS(ID)
            ON DELETE CASCADE,
    CONSTRAINT fk_user_tenants_role
        FOREIGN KEY (ROLE_ID)
            REFERENCES ROLES(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
