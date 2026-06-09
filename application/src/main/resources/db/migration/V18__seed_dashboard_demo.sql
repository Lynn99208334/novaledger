-- ========================================
-- NovaLedger Seed Data
-- Flyway Migration V18
-- Dashboard Demo Data
-- 目的：讓 Alice 的 Dashboard 有完整測試資料
--   - 新增 USD 美金帳戶
--   - 更新台幣帳戶餘額
--   - 本月收入交易（薪資）
--   - 本月支出交易（飲食/交通/居家/娛樂/醫療）
--   - transaction_items 帶分類 id
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 1. 新增 Alice 的 USD 美金帳戶
-- ========================================
INSERT INTO user_accounts (
    TENANT_ID, USER_ID,
    ACCOUNT_TYPE, BANK_CODE, BRANCH_ID,
    ACCOUNT_NUMBER, NAME,
    CURRENCY_CODE, CURRENT_BALANCE, INITIAL_BALANCE,
    NOTES, CREATED_AT, UPDATED_AT, DELETED_AT
)
VALUES (
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM users   WHERE USERNAME = 'alice'),
    'BANK', '808', NULL,
    NULL,
    '玉山銀行 美金帳戶',
    'USD', 3200.00, 3200.00,
    'Demo 美金帳戶',
    NOW(), NOW(), NULL
);

-- ========================================
-- 2. 更新台幣帳戶餘額（玉山台幣）
-- ========================================
UPDATE user_accounts
SET CURRENT_BALANCE = 85000.00, INITIAL_BALANCE = 85000.00, UPDATED_AT = NOW()
WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
  AND CURRENCY_CODE = 'TWD'
  AND NAME = '玉山銀行 臺幣綜存';

UPDATE user_accounts
SET CURRENT_BALANCE = 32000.00, INITIAL_BALANCE = 32000.00, UPDATED_AT = NOW()
WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
  AND CURRENCY_CODE = 'TWD'
  AND NAME = '中信銀行 活期存款';

UPDATE user_accounts
SET CURRENT_BALANCE = 15500.00, INITIAL_BALANCE = 15500.00, UPDATED_AT = NOW()
WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
  AND CURRENCY_CODE = 'TWD'
  AND NAME = '永豐銀行 活期儲蓄存款';

-- ========================================
-- 3. 本月收入交易（薪資）
-- ========================================
INSERT INTO transactions (
    TENANT_ID, USER_ID, ACCOUNT_ID, CREDIT_CARD_ID,
    TX_TYPE_CODE, TRANSACTION_DATE, TOTAL_AMOUNT, CURRENCY_CODE, MEMO,
    CREATED_AT, UPDATED_AT, DELETED_AT
) VALUES (
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM users   WHERE USERNAME = 'alice'),
    (SELECT ID FROM user_accounts
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
       AND NAME = '玉山銀行 臺幣綜存'),
    NULL,
    'INCOME',
    DATE_FORMAT(NOW(), '%Y-%m-05'),
    65000.00, 'TWD', '四月薪資',
    NOW(), NOW(), NULL
);

INSERT INTO transaction_items (TENANT_ID, TRANSACTION_ID, CATEGORY_ID, AMOUNT, MEMO, CREATED_AT)
SELECT
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM transactions
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
       AND MEMO = '四月薪資' AND TX_TYPE_CODE = 'INCOME'),
    (SELECT C.ID FROM categories C
     JOIN tenants T ON T.ID = C.TENANT_ID
     WHERE T.CODE = 'alice-personal' AND C.NAME = '月薪' AND C.TYPE = 'INCOME'),
    65000.00, '月薪', NOW();

-- 投資收益（股利）
INSERT INTO transactions (
    TENANT_ID, USER_ID, ACCOUNT_ID, CREDIT_CARD_ID,
    TX_TYPE_CODE, TRANSACTION_DATE, TOTAL_AMOUNT, CURRENCY_CODE, MEMO,
    CREATED_AT, UPDATED_AT, DELETED_AT
) VALUES (
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM users   WHERE USERNAME = 'alice'),
    (SELECT ID FROM user_accounts
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
       AND NAME = '玉山銀行 美金帳戶'),
    NULL,
    'INCOME',
    DATE_FORMAT(NOW(), '%Y-%m-10'),
    200.00, 'USD', 'ETF 股利',
    NOW(), NOW(), NULL
);

INSERT INTO transaction_items (TENANT_ID, TRANSACTION_ID, CATEGORY_ID, AMOUNT, MEMO, CREATED_AT)
SELECT
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM transactions
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
       AND MEMO = 'ETF 股利' AND TX_TYPE_CODE = 'INCOME'),
    (SELECT C.ID FROM categories C
     JOIN tenants T ON T.ID = C.TENANT_ID
     WHERE T.CODE = 'alice-personal' AND C.NAME = '股利' AND C.TYPE = 'INCOME'),
    200.00, 'USD 股利', NOW();

-- ========================================
-- 4. 本月支出交易
-- ========================================

-- 飲食支出
INSERT INTO transactions (
    TENANT_ID, USER_ID, ACCOUNT_ID, CREDIT_CARD_ID,
    TX_TYPE_CODE, TRANSACTION_DATE, TOTAL_AMOUNT, CURRENCY_CODE, MEMO,
    CREATED_AT, UPDATED_AT, DELETED_AT
) VALUES (
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM users   WHERE USERNAME = 'alice'),
    (SELECT ID FROM user_accounts
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
       AND NAME = '玉山銀行 臺幣綜存'),
    NULL,
    'EXPENSE',
    DATE_FORMAT(NOW(), '%Y-%m-08'),
    8500.00, 'TWD', '本月飲食',
    NOW(), NOW(), NULL
);

INSERT INTO transaction_items (TENANT_ID, TRANSACTION_ID, CATEGORY_ID, AMOUNT, MEMO, CREATED_AT)
SELECT
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM transactions
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
       AND MEMO = '本月飲食'),
    (SELECT C.ID FROM categories C
     JOIN tenants T ON T.ID = C.TENANT_ID
     WHERE T.CODE = 'alice-personal' AND C.NAME = '飲食' AND C.TYPE = 'EXPENSE' AND C.PARENT_ID IS NULL),
    8500.00, '飲食費用', NOW();

-- 交通支出
INSERT INTO transactions (
    TENANT_ID, USER_ID, ACCOUNT_ID, CREDIT_CARD_ID,
    TX_TYPE_CODE, TRANSACTION_DATE, TOTAL_AMOUNT, CURRENCY_CODE, MEMO,
    CREATED_AT, UPDATED_AT, DELETED_AT
) VALUES (
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM users   WHERE USERNAME = 'alice'),
    (SELECT ID FROM user_accounts
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
       AND NAME = '永豐銀行 活期儲蓄存款'),
    NULL,
    'EXPENSE',
    DATE_FORMAT(NOW(), '%Y-%m-12'),
    2200.00, 'TWD', '本月交通',
    NOW(), NOW(), NULL
);

INSERT INTO transaction_items (TENANT_ID, TRANSACTION_ID, CATEGORY_ID, AMOUNT, MEMO, CREATED_AT)
SELECT
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM transactions
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
       AND MEMO = '本月交通'),
    (SELECT C.ID FROM categories C
     JOIN tenants T ON T.ID = C.TENANT_ID
     WHERE T.CODE = 'alice-personal' AND C.NAME = '交通' AND C.TYPE = 'EXPENSE' AND C.PARENT_ID IS NULL),
    2200.00, '交通費用', NOW();

-- 居家生活（房租）
INSERT INTO transactions (
    TENANT_ID, USER_ID, ACCOUNT_ID, CREDIT_CARD_ID,
    TX_TYPE_CODE, TRANSACTION_DATE, TOTAL_AMOUNT, CURRENCY_CODE, MEMO,
    CREATED_AT, UPDATED_AT, DELETED_AT
) VALUES (
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM users   WHERE USERNAME = 'alice'),
    (SELECT ID FROM user_accounts
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
       AND NAME = '中信銀行 活期存款'),
    NULL,
    'EXPENSE',
    DATE_FORMAT(NOW(), '%Y-%m-01'),
    18000.00, 'TWD', '四月房租',
    NOW(), NOW(), NULL
);

INSERT INTO transaction_items (TENANT_ID, TRANSACTION_ID, CATEGORY_ID, AMOUNT, MEMO, CREATED_AT)
SELECT
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM transactions
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
       AND MEMO = '四月房租'),
    (SELECT C.ID FROM categories C
     JOIN tenants T ON T.ID = C.TENANT_ID
     WHERE T.CODE = 'alice-personal' AND C.NAME = '居家生活' AND C.TYPE = 'EXPENSE' AND C.PARENT_ID IS NULL),
    18000.00, '房租', NOW();

-- 娛樂支出
INSERT INTO transactions (
    TENANT_ID, USER_ID, ACCOUNT_ID, CREDIT_CARD_ID,
    TX_TYPE_CODE, TRANSACTION_DATE, TOTAL_AMOUNT, CURRENCY_CODE, MEMO,
    CREATED_AT, UPDATED_AT, DELETED_AT
) VALUES (
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM users   WHERE USERNAME = 'alice'),
    (SELECT ID FROM user_accounts
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
       AND NAME = '玉山銀行 臺幣綜存'),
    NULL,
    'EXPENSE',
    DATE_FORMAT(NOW(), '%Y-%m-15'),
    3600.00, 'TWD', '本月娛樂',
    NOW(), NOW(), NULL
);

INSERT INTO transaction_items (TENANT_ID, TRANSACTION_ID, CATEGORY_ID, AMOUNT, MEMO, CREATED_AT)
SELECT
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM transactions
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
       AND MEMO = '本月娛樂'),
    (SELECT C.ID FROM categories C
     JOIN tenants T ON T.ID = C.TENANT_ID
     WHERE T.CODE = 'alice-personal' AND C.NAME = '娛樂' AND C.TYPE = 'EXPENSE' AND C.PARENT_ID IS NULL),
    3600.00, '娛樂費用', NOW();

-- 醫療健康
INSERT INTO transactions (
    TENANT_ID, USER_ID, ACCOUNT_ID, CREDIT_CARD_ID,
    TX_TYPE_CODE, TRANSACTION_DATE, TOTAL_AMOUNT, CURRENCY_CODE, MEMO,
    CREATED_AT, UPDATED_AT, DELETED_AT
) VALUES (
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM users   WHERE USERNAME = 'alice'),
    (SELECT ID FROM user_accounts
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
       AND NAME = '玉山銀行 臺幣綜存'),
    NULL,
    'EXPENSE',
    DATE_FORMAT(NOW(), '%Y-%m-18'),
    1200.00, 'TWD', '看診費用',
    NOW(), NOW(), NULL
);

INSERT INTO transaction_items (TENANT_ID, TRANSACTION_ID, CATEGORY_ID, AMOUNT, MEMO, CREATED_AT)
SELECT
    (SELECT ID FROM tenants WHERE CODE = 'alice-personal'),
    (SELECT ID FROM transactions
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'alice-personal')
       AND MEMO = '看診費用'),
    (SELECT C.ID FROM categories C
     JOIN tenants T ON T.ID = C.TENANT_ID
     WHERE T.CODE = 'alice-personal' AND C.NAME = '醫療健康' AND C.TYPE = 'EXPENSE' AND C.PARENT_ID IS NULL),
    1200.00, '醫療', NOW();


-- ========================================
-- Son Dashboard Demo Data
-- 目的：讓 Son 的個人帳 Dashboard 有完整測試資料
-- ========================================

-- ========================================
-- 1. 更新 Son 帳戶餘額
-- ========================================

UPDATE user_accounts
SET CURRENT_BALANCE = 5000.00,
    INITIAL_BALANCE = 5000.00,
    UPDATED_AT = NOW()
WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'son-personal')
  AND NAME = 'Son 零用金帳戶';

UPDATE user_accounts
SET CURRENT_BALANCE = 25000.00,
    INITIAL_BALANCE = 25000.00,
    UPDATED_AT = NOW()
WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'son-personal')
  AND NAME = 'Son 教育存款';

-- ========================================
-- 2. Son 本月收入：零用錢
-- ========================================

INSERT INTO transactions (
    TENANT_ID, USER_ID, ACCOUNT_ID, CREDIT_CARD_ID,
    TX_TYPE_CODE, TRANSACTION_DATE, TOTAL_AMOUNT, CURRENCY_CODE, MEMO,
    CREATED_AT, UPDATED_AT, DELETED_AT
) VALUES (
             (SELECT ID FROM tenants WHERE CODE = 'son-personal'),
             (SELECT ID FROM users   WHERE USERNAME = 'son'),
             (SELECT ID FROM user_accounts
              WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'son-personal')
                AND NAME = 'Son 零用金帳戶'),
             NULL,
             'INCOME',
             DATE_FORMAT(NOW(), '%Y-%m-03'),
             3000.00, 'TWD', '本月零用錢',
             NOW(), NOW(), NULL
         );

INSERT INTO transaction_items (
    TENANT_ID, TRANSACTION_ID, CATEGORY_ID, AMOUNT, MEMO, CREATED_AT
)
SELECT
    (SELECT ID FROM tenants WHERE CODE = 'son-personal'),
    (SELECT ID FROM transactions
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'son-personal')
       AND MEMO = '本月零用錢'
       AND TX_TYPE_CODE = 'INCOME'),
    (SELECT C.ID FROM categories C
                          JOIN tenants T ON T.ID = C.TENANT_ID
     WHERE T.CODE = 'son-personal'
       AND C.NAME = '其他收入'
       AND C.TYPE = 'INCOME'
       AND C.PARENT_ID IS NULL),
    3000.00,
    '零用錢',
    NOW();

-- ========================================
-- 3. Son 本月支出：飲食
-- ========================================

INSERT INTO transactions (
    TENANT_ID, USER_ID, ACCOUNT_ID, CREDIT_CARD_ID,
    TX_TYPE_CODE, TRANSACTION_DATE, TOTAL_AMOUNT, CURRENCY_CODE, MEMO,
    CREATED_AT, UPDATED_AT, DELETED_AT
) VALUES (
             (SELECT ID FROM tenants WHERE CODE = 'son-personal'),
             (SELECT ID FROM users   WHERE USERNAME = 'son'),
             (SELECT ID FROM user_accounts
              WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'son-personal')
                AND NAME = 'Son 零用金帳戶'),
             NULL,
             'EXPENSE',
             DATE_FORMAT(NOW(), '%Y-%m-07'),
             800.00, 'TWD', 'Son 本月飲食',
             NOW(), NOW(), NULL
         );

INSERT INTO transaction_items (
    TENANT_ID, TRANSACTION_ID, CATEGORY_ID, AMOUNT, MEMO, CREATED_AT
)
SELECT
    (SELECT ID FROM tenants WHERE CODE = 'son-personal'),
    (SELECT ID FROM transactions
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'son-personal')
       AND MEMO = 'Son 本月飲食'
       AND TX_TYPE_CODE = 'EXPENSE'),
    (SELECT C.ID FROM categories C
                          JOIN tenants T ON T.ID = C.TENANT_ID
     WHERE T.CODE = 'son-personal'
       AND C.NAME = '飲食'
       AND C.TYPE = 'EXPENSE'
       AND C.PARENT_ID IS NULL),
    800.00,
    'Son 飲食支出',
    NOW();

-- ========================================
-- 4. Son 本月支出：娛樂
-- ========================================

INSERT INTO transactions (
    TENANT_ID, USER_ID, ACCOUNT_ID, CREDIT_CARD_ID,
    TX_TYPE_CODE, TRANSACTION_DATE, TOTAL_AMOUNT, CURRENCY_CODE, MEMO,
    CREATED_AT, UPDATED_AT, DELETED_AT
) VALUES (
             (SELECT ID FROM tenants WHERE CODE = 'son-personal'),
             (SELECT ID FROM users   WHERE USERNAME = 'son'),
             (SELECT ID FROM user_accounts
              WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'son-personal')
                AND NAME = 'Son 零用金帳戶'),
             NULL,
             'EXPENSE',
             DATE_FORMAT(NOW(), '%Y-%m-11'),
             1200.00, 'TWD', 'Son 本月娛樂',
             NOW(), NOW(), NULL
         );

INSERT INTO transaction_items (
    TENANT_ID, TRANSACTION_ID, CATEGORY_ID, AMOUNT, MEMO, CREATED_AT
)
SELECT
    (SELECT ID FROM tenants WHERE CODE = 'son-personal'),
    (SELECT ID FROM transactions
     WHERE TENANT_ID = (SELECT ID FROM tenants WHERE CODE = 'son-personal')
       AND MEMO = 'Son 本月娛樂'
       AND TX_TYPE_CODE = 'EXPENSE'),
    (SELECT C.ID FROM categories C
                          JOIN tenants T ON T.ID = C.TENANT_ID
     WHERE T.CODE = 'son-personal'
       AND C.NAME = '娛樂'
       AND C.TYPE = 'EXPENSE'
       AND C.PARENT_ID IS NULL),
    1200.00,
    'Son 娛樂支出',
    NOW();

SET FOREIGN_KEY_CHECKS = 1;
