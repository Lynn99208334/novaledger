-- ========================================
-- NovaLedger Seed Data
-- Flyway Migration V21
-- admin 帳號補建 tenant + user_tenants
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- admin 的個人帳本
INSERT INTO tenants (
    CODE, NAME, TYPE, PLAN,
    OWNER_USER_ID, STATUS,
    CREATED_AT, UPDATED_AT, DELETED_AT
)
SELECT
    'admin-personal',
    'Admin 的管理帳本',
    'PERSONAL', 'FREE',
    u.ID,
    'ACTIVE',
    NOW(), NOW(), NULL
FROM users u
WHERE u.USERNAME = 'sys_admin'
  AND NOT EXISTS (
    SELECT 1 FROM tenants t WHERE t.CODE = 'admin-personal'
);

-- admin → admin-personal（MEMBER role）
INSERT INTO user_tenants (
    USER_ID,
    TENANT_ID,
    ROLE_ID,
    STATUS,
    JOINED_AT
)
SELECT
    u.ID,
    t.ID,
    r.ID,
    'ACTIVE',
    NOW()
FROM users u
         JOIN tenants t ON t.CODE = 'admin-personal'
         JOIN roles r ON r.CODE = 'MEMBER'
WHERE u.USERNAME = 'sys_admin'
  AND NOT EXISTS (
    SELECT 1
    FROM user_tenants ut
    WHERE ut.USER_ID = u.ID
      AND ut.TENANT_ID = t.ID
      AND ut.DELETED_AT IS NULL
);

SET FOREIGN_KEY_CHECKS = 1;
