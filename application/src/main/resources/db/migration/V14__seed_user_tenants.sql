-- ========================================
-- NovaLedger Seed Data
-- Flyway Migration V14 (SAFE VERSION)
-- 測試用 USER_TENANTS（可重跑 / 防錯）
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- Alice → alice-personal（MEMBER）
-- ========================================
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
         JOIN tenants t ON t.CODE = 'alice-personal'
         JOIN roles r ON r.CODE = 'MEMBER'
WHERE u.USERNAME = 'alice'
  AND NOT EXISTS (
    SELECT 1
    FROM user_tenants ut
    WHERE ut.USER_ID = u.ID
      AND ut.TENANT_ID = t.ID
      AND ut.DELETED_AT IS NULL
);

-- ========================================
-- Bob → bob-personal（MEMBER）
-- ========================================
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
         JOIN tenants t ON t.CODE = 'bob-personal'
         JOIN roles r ON r.CODE = 'MEMBER'
WHERE u.USERNAME = 'bob'
  AND NOT EXISTS (
    SELECT 1
    FROM user_tenants ut
    WHERE ut.USER_ID = u.ID
      AND ut.TENANT_ID = t.ID
      AND ut.DELETED_AT IS NULL
);

-- ========================================
-- Son → son-personal（MEMBER）
-- ========================================
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
         JOIN tenants t ON t.CODE = 'son-personal'
         JOIN roles r ON r.CODE = 'MEMBER'
WHERE u.USERNAME = 'son'
  AND NOT EXISTS (
    SELECT 1
    FROM user_tenants ut
    WHERE ut.USER_ID = u.ID
      AND ut.TENANT_ID = t.ID
      AND ut.DELETED_AT IS NULL
);

SET FOREIGN_KEY_CHECKS = 1;