-- ========================================
-- NovaLedger Seed Data
-- Flyway Migration V12
-- USERS + TENANTS（含 son）
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- USERS
-- ========================================

INSERT INTO users (
    USERNAME, EMAIL, PASSWORD,
    ENABLED, STATUS, EMAIL_VERIFIED, VERIFIED_AT,
    EMAIL_VERIFY_TOKEN, EMAIL_VERIFY_EXPIRED_AT, EMAIL_VERIFY_LAST_SENT_AT,
    IS_SYSTEM_ADMIN, LAST_LOGIN_AT,
    CREATED_AT, UPDATED_AT, DELETED_AT
) VALUES
-- 系統管理員
(
    'sys_admin',
    'admin@novaledger.dev',
    '$2a$10$8R0nwfQG.dSGE9UemN0JveorFQPg6QzlTyWg/WNFnnGQyi6eCgn4C',
    TRUE, 'ACTIVE', TRUE, '2026-01-01 00:00:00',
    NULL, NULL, NULL,
    TRUE, NULL,
    NOW(), NOW(), NULL
),
-- Alice
(
    'alice',
    'alice@novaledger.dev',
    '$2a$10$8R0nwfQG.dSGE9UemN0JveorFQPg6QzlTyWg/WNFnnGQyi6eCgn4C',
    TRUE, 'ACTIVE', TRUE, '2026-01-01 00:00:00',
    NULL, NULL, NULL,
    FALSE, NULL,
    NOW(), NOW(), NULL
),
-- Bob
(
    'bob',
    'bob@novaledger.dev',
    '$2a$10$8R0nwfQG.dSGE9UemN0JveorFQPg6QzlTyWg/WNFnnGQyi6eCgn4C',
    TRUE, 'ACTIVE', TRUE, '2026-01-01 00:00:00',
    NULL, NULL, NULL,
    FALSE, NULL,
    NOW(), NOW(), NULL
),
-- ✅ Son（新增）
(
    'son',
    'son@novaledger.dev',
    '$2a$10$8R0nwfQG.dSGE9UemN0JveorFQPg6QzlTyWg/WNFnnGQyi6eCgn4C',
    TRUE, 'ACTIVE', TRUE, '2026-01-01 00:00:00',
    NULL, NULL, NULL,
    FALSE, NULL,
    NOW(), NOW(), NULL
);

-- ========================================
-- TENANTS
-- ========================================

INSERT INTO tenants (
    CODE, NAME, TYPE, PLAN,
    OWNER_USER_ID, STATUS,
    CREATED_AT, UPDATED_AT, DELETED_AT
) VALUES
-- Alice 個人帳
(
    'alice-personal',
    'Alice 的個人帳本',
    'PERSONAL', 'FREE',
    (SELECT ID FROM users WHERE USERNAME = 'alice'),
    'ACTIVE',
    NOW(), NOW(), NULL
),
-- Bob 個人帳
(
    'bob-personal',
    'Bob 的個人帳本',
    'PERSONAL', 'FREE',
    (SELECT ID FROM users WHERE USERNAME = 'bob'),
    'ACTIVE',
    NOW(), NOW(), NULL
),
-- Son 個人帳
(
    'son-personal',
    'Son 的個人帳本',
    'PERSONAL', 'FREE',
    (SELECT ID FROM users WHERE USERNAME = 'son'),
    'ACTIVE',
    NOW(), NOW(), NULL
);

SET FOREIGN_KEY_CHECKS = 1;