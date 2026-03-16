-- ========================================
-- NovaLedger Seed Data
-- Flyway Migration V12
-- 測試用 USERS + TENANTS
--
-- 所有帳號密碼均為：password123
-- bcrypt hash (cost=10)：
--   $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 測試使用者
-- ========================================

INSERT INTO USERS (
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
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    TRUE, 'ACTIVE', TRUE, '2026-01-01 00:00:00',
    NULL, NULL, NULL,
    TRUE, NULL,
    NOW(), NOW(), NULL
),
-- 一般測試用戶 Alice
(
    'alice',
    'alice@novaledger.dev',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    TRUE, 'ACTIVE', TRUE, '2026-01-01 00:00:00',
    NULL, NULL, NULL,
    FALSE, NULL,
    NOW(), NOW(), NULL
),
-- 一般測試用戶 Bob
(
    'bob',
    'bob@novaledger.dev',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    TRUE, 'ACTIVE', TRUE, '2026-01-01 00:00:00',
    NULL, NULL, NULL,
    FALSE, NULL,
    NOW(), NOW(), NULL
);

-- ========================================
-- 測試租戶（個人帳本）
-- ========================================

INSERT INTO TENANTS (
    CODE, NAME, TYPE, PLAN,
    OWNER_USER_ID, STATUS,
    CREATED_AT, UPDATED_AT, DELETED_AT
) VALUES
-- Alice 的個人帳本
(
    'alice-personal',
    'Alice 的個人帳本',
    'PERSONAL', 'FREE',
    (SELECT ID FROM USERS WHERE USERNAME = 'alice'),
    'ACTIVE',
    NOW(), NOW(), NULL
),
-- Bob 的個人帳本
(
    'bob-personal',
    'Bob 的個人帳本',
    'PERSONAL', 'FREE',
    (SELECT ID FROM USERS WHERE USERNAME = 'bob'),
    'ACTIVE',
    NOW(), NOW(), NULL
);

SET FOREIGN_KEY_CHECKS = 1;
