-- ========================================
-- NovaLedger Seed Data
-- Flyway Migration V17
-- Family Tenant Demo Seed
--
-- 目的：
-- 1. 建立 alice-family 家庭帳 tenant
-- 2. 建立 alice / son 的家庭帳成員關聯
-- 3. 建立家庭帳 Demo 用帳戶
--
-- 注意：
-- - bob 保留為個人帳測試用，不加入 alice-family
-- - 家庭帳戶統一由 alice 建立
-- - son 透過 user_tenants 關聯看到同一個 family tenant 資料
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 1. 建立 tenant（家庭帳）
-- ========================================

INSERT INTO tenants (
    CODE,
    NAME,
    TYPE,
    PLAN,
    OWNER_USER_ID,
    STATUS,
    CREATED_AT,
    UPDATED_AT,
    DELETED_AT
)
SELECT
    'alice-family',
    'Alice Family',
    'FAMILY',
    'FREE',
    u.ID,
    'ACTIVE',
    NOW(),
    NOW(),
    NULL
FROM users u
WHERE u.USERNAME = 'alice'
  AND NOT EXISTS (
    SELECT 1
    FROM tenants t
    WHERE t.CODE = 'alice-family'
);

-- ========================================
-- 2. 建立 user_tenants（家庭成員）
-- alice = OWNER
-- son   = MEMBER
-- ========================================

INSERT INTO user_tenants (
    USER_ID,
    TENANT_ID,
    ROLE_ID,
    STATUS,
    JOINED_AT,
    REMOVED_BY_USER_ID,
    DELETED_AT
)
SELECT
    u.ID,
    t.ID,
    r.ID,
    'ACTIVE',
    NOW(),
    NULL,
    NULL
FROM users u
         JOIN tenants t
              ON t.CODE = 'alice-family'
         JOIN roles r
              ON r.CODE =
                 CASE
                     WHEN u.USERNAME = 'alice' THEN 'OWNER'
                     WHEN u.USERNAME = 'son' THEN 'MEMBER'
                     END
WHERE u.USERNAME IN ('alice', 'son')
  AND NOT EXISTS (
    SELECT 1
    FROM user_tenants ut
    WHERE ut.USER_ID = u.ID
      AND ut.TENANT_ID = t.ID
      AND ut.DELETED_AT IS NULL
);

-- ========================================
-- 3. 建立家庭帳戶
-- TENANT_ID = alice-family
-- USER_ID   = alice
-- ========================================

-- 家庭現金帳戶
INSERT INTO user_accounts (
    TENANT_ID,
    USER_ID,
    ACCOUNT_TYPE,
    BANK_CODE,
    BRANCH_ID,
    ACCOUNT_NUMBER,
    NAME,
    CURRENCY_CODE,
    CURRENT_BALANCE,
    INITIAL_BALANCE,
    NOTES,
    CREATED_AT,
    UPDATED_AT,
    DELETED_AT
)
SELECT
    t.ID,
    u.ID,
    'CASH',
    NULL,
    NULL,
    NULL,
    '家庭現金帳戶',
    'TWD',
    30000.00,
    30000.00,
    'Family demo cash account',
    NOW(),
    NOW(),
    NULL
FROM tenants t
         JOIN users u
              ON u.USERNAME = 'alice'
WHERE t.CODE = 'alice-family'
  AND NOT EXISTS (
    SELECT 1
    FROM user_accounts ua
    WHERE ua.TENANT_ID = t.ID
      AND ua.NAME = '家庭現金帳戶'
      AND ua.DELETED_AT IS NULL
);

-- 家庭銀行帳戶
INSERT INTO user_accounts (
    TENANT_ID,
    USER_ID,
    ACCOUNT_TYPE,
    BANK_CODE,
    BRANCH_ID,
    ACCOUNT_NUMBER,
    NAME,
    CURRENCY_CODE,
    CURRENT_BALANCE,
    INITIAL_BALANCE,
    NOTES,
    CREATED_AT,
    UPDATED_AT,
    DELETED_AT
)
SELECT
    t.ID,
    u.ID,
    'BANK',
    '004',
    (SELECT ID FROM bank_branches WHERE BRANCH_CODE = '0041001'),
    '004-000-000001',
    '家庭銀行帳戶',
    'TWD',
    120000.00,
    120000.00,
    'Family demo bank account',
    NOW(),
    NOW(),
    NULL
FROM tenants t
         JOIN users u
              ON u.USERNAME = 'alice'
WHERE t.CODE = 'alice-family'
  AND NOT EXISTS (
    SELECT 1
    FROM user_accounts ua
    WHERE ua.TENANT_ID = t.ID
      AND ua.NAME = '家庭銀行帳戶'
      AND ua.DELETED_AT IS NULL
);

-- 兒子教育基金
INSERT INTO user_accounts (
    TENANT_ID,
    USER_ID,
    ACCOUNT_TYPE,
    BANK_CODE,
    BRANCH_ID,
    ACCOUNT_NUMBER,
    NAME,
    CURRENCY_CODE,
    CURRENT_BALANCE,
    INITIAL_BALANCE,
    NOTES,
    CREATED_AT,
    UPDATED_AT,
    DELETED_AT
)
SELECT
    t.ID,
    u.ID,
    'BANK',
    '004',
    (SELECT ID FROM bank_branches WHERE BRANCH_CODE = '0041002'),
    '004-000-000002',
    '兒子教育基金',
    'TWD',
    80000.00,
    80000.00,
    'Family demo education fund',
    NOW(),
    NOW(),
    NULL
FROM tenants t
         JOIN users u
              ON u.USERNAME = 'alice'
WHERE t.CODE = 'alice-family'
  AND NOT EXISTS (
    SELECT 1
    FROM user_accounts ua
    WHERE ua.TENANT_ID = t.ID
      AND ua.NAME = '兒子教育基金'
      AND ua.DELETED_AT IS NULL
);

SET FOREIGN_KEY_CHECKS = 1;