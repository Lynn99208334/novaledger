-- ========================================
-- NovaLedger Seed Data
-- Flyway Migration V15
-- 測試用 USER_ACCOUNTS
-- Alice 的玉山、中信、永豐帳戶
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO user_accounts (
    TENANT_ID, USER_ID,
    ACCOUNT_TYPE, BANK_CODE, BRANCH_ID,
    ACCOUNT_NUMBER, NAME,
    CURRENCY_CODE, CURRENT_BALANCE, INITIAL_BALANCE,
    NOTES, CREATED_AT, UPDATED_AT, DELETED_AT
)
VALUES
-- 玉山銀行（808）
(
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM users   WHERE USERNAME = 'alice'),
    'BANK', '808',
    (SELECT ID FROM bank_branches WHERE BRANCH_CODE = '8081003'),
    '0820979072810',
    '玉山銀行 臺幣綜存',
    'TWD', 0.00, 0.00,
    '測試帳戶 - 玉山存摺 XLS 格式',
    NOW(), NOW(), NULL
),
-- 中國信託（822）
(
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM users   WHERE USERNAME = 'alice'),
    'BANK', '822',
    (SELECT ID FROM bank_branches WHERE BRANCH_CODE = '8221001'),
    NULL,
    '中信銀行 活期存款',
    'TWD', 0.00, 0.00,
    '測試帳戶 - 中信 CSV 格式',
    NOW(), NOW(), NULL
),
-- 永豐銀行（無 bank_code，先用 NULL）
(
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM users   WHERE USERNAME = 'alice'),
    'BANK', '807', NULL,
    '169-018-0004153-6',
    '永豐銀行 活期儲蓄存款',
    'TWD', 0.00, 0.00,
    '測試帳戶 - 永豐 CSV 格式',
    NOW(), NOW(), NULL
);

-- ========================================
-- Son 個人帳戶
-- ========================================

-- Son 零用金帳戶
INSERT INTO user_accounts (
    TENANT_ID, USER_ID,
    ACCOUNT_TYPE, BANK_CODE, BRANCH_ID,
    ACCOUNT_NUMBER, NAME,
    CURRENCY_CODE, CURRENT_BALANCE, INITIAL_BALANCE,
    NOTES, CREATED_AT, UPDATED_AT, DELETED_AT
)
VALUES
    (
        (SELECT ID FROM tenants WHERE CODE = 'son-personal'),
        (SELECT ID FROM users   WHERE USERNAME = 'son'),
        'CASH', NULL, NULL,
        NULL,
        'Son 零用金帳戶',
        'TWD', 5000.00, 5000.00,
        '測試帳戶 - Son 個人零用金',
        NOW(), NOW(), NULL
    );

-- Son 教育存款
INSERT INTO user_accounts (
    TENANT_ID, USER_ID,
    ACCOUNT_TYPE, BANK_CODE, BRANCH_ID,
    ACCOUNT_NUMBER, NAME,
    CURRENCY_CODE, CURRENT_BALANCE, INITIAL_BALANCE,
    NOTES, CREATED_AT, UPDATED_AT, DELETED_AT
)
VALUES
    (
        (SELECT ID FROM tenants WHERE CODE = 'son-personal'),
        (SELECT ID FROM users   WHERE USERNAME = 'son'),
        'BANK', '004',
        (SELECT ID FROM bank_branches WHERE BRANCH_CODE = '0041001'),
        '004-000-000003',
        'Son 教育存款',
        'TWD', 25000.00, 25000.00,
        '測試帳戶 - Son 個人教育存款',
        NOW(), NOW(), NULL
    );

SET FOREIGN_KEY_CHECKS = 1;