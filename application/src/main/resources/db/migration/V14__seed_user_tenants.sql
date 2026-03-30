-- ========================================
-- NovaLedger Seed Data
-- Flyway Migration V14
-- 測試用 USER_TENANTS
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO user_tenants (USER_ID, TENANT_ID, ROLE_ID, STATUS, JOINED_AT)
VALUES
-- Alice 加入 alice-personal，角色 MEMBER
(
    (SELECT ID FROM users   WHERE USERNAME = 'alice'),
    (SELECT ID FROM tenants WHERE CODE     = 'alice-personal'),
    (SELECT ID FROM roles   WHERE CODE     = 'MEMBER'),
    'ACTIVE',
    NOW()
),
-- Bob 加入 bob-personal，角色 MEMBER
(
    (SELECT ID FROM users   WHERE USERNAME = 'bob'),
    (SELECT ID FROM tenants WHERE CODE     = 'bob-personal'),
    (SELECT ID FROM roles   WHERE CODE     = 'MEMBER'),
    'ACTIVE',
    NOW()
);

SET FOREIGN_KEY_CHECKS = 1;
