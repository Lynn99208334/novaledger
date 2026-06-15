-- ========================================
-- NovaLedger Schema
-- Flyway Migration V3
-- System Master Data：CURRENCIES、INVESTMENT_TYPES、
--                     TRANSACTION_TYPES、BANKS、BANK_BRANCHES
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- CURRENCIES
-- 設計重點：
--   全域系統主檔（非 tenant-aware）
--   CODE 為 ISO 4217 標準，直接當 PK
--   不支援軟刪除，以 IS_ACTIVE 控制
-- ========================================

CREATE TABLE currencies (
    -- PK
                            CODE           CHAR(3)     NOT NULL,                     -- ISO 4217 幣別代碼，如 TWD / USD

    -- 顯示資訊
                            NAME           VARCHAR(50) NOT NULL,                     -- 幣別顯示名稱，如 新台幣
                            SYMBOL         VARCHAR(10),                              -- 顯示符號，如 NT$（Optional）
                            DECIMAL_PLACES TINYINT     NOT NULL,                     -- 顯示用精度，TWD=0 / USD=2

    -- 狀態
                            IS_ACTIVE      BOOLEAN     NOT NULL,                     -- 是否可使用

    -- 稽核欄位
                            CREATED_AT     DATETIME    NOT NULL,                     -- 建立時間
                            UPDATED_AT     DATETIME    NOT NULL,                     -- 更新時間

                            PRIMARY KEY (CODE),

    -- Index
                            INDEX idx_currency_active           (IS_ACTIVE)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO currencies (CODE, NAME, SYMBOL, DECIMAL_PLACES, IS_ACTIVE, CREATED_AT, UPDATED_AT) VALUES
                                                                                                   ('TWD', '新台幣', 'NT$', 0, TRUE, NOW(), NOW()),
                                                                                                   ('USD', '美元',   '$',   2, TRUE, NOW(), NOW()),
                                                                                                   ('JPY', '日圓',   '¥',   0, TRUE, NOW(), NOW()),
                                                                                                   ('EUR', '歐元',   '€',   2, TRUE, NOW(), NOW()),
                                                                                                   ('CNY', '人民幣', '¥',   2, TRUE, NOW(), NOW());


-- ========================================
-- INVESTMENT_TYPES
-- 設計重點：
--   系統主檔（非 tenant-aware）
--   加 ID 當 PK，CODE 加 UNIQUE（保留未來租戶自訂擴充彈性）
--   IS_SYSTEM 控制是否為系統內建
--   IS_ACTIVE 控制是否可使用
-- ========================================

CREATE TABLE investment_types (
    -- PK
                                  ID          BIGINT       AUTO_INCREMENT PRIMARY KEY,     -- 主鍵

    -- 識別
                                  CODE        VARCHAR(30)  NOT NULL,                       -- 類型識別碼，如 BANK_ACCOUNT
                                  NAME        VARCHAR(50)  NOT NULL,                       -- 類型名稱，如 銀行帳戶
                                  DESCRIPTION VARCHAR(255),                                -- 類型用途說明（Optional）

    -- 控制
                                  IS_SYSTEM   BOOLEAN      NOT NULL,                       -- 是否系統定義
                                  IS_ACTIVE   BOOLEAN      NOT NULL,                       -- 是否可使用

    -- 稽核欄位
                                  CREATED_AT  DATETIME     NOT NULL,                       -- 建立時間
                                  UPDATED_AT  DATETIME     NOT NULL,                       -- 更新時間

    -- Unique
                                  UNIQUE KEY uk_investment_type_code  (CODE),

    -- Index
                                  INDEX idx_investment_type_active    (IS_ACTIVE)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO investment_types (CODE, NAME, DESCRIPTION, IS_SYSTEM, IS_ACTIVE, CREATED_AT, UPDATED_AT) VALUES
                                                                                                         ('BANK_ACCOUNT', '銀行帳戶', '一般銀行存款帳戶', TRUE, TRUE, NOW(), NOW()),
                                                                                                         ('CASH',         '現金',     '現金資產',         TRUE, TRUE, NOW(), NOW()),
                                                                                                         ('STOCK',        '股票',     '上市股票投資',     TRUE, TRUE, NOW(), NOW()),
                                                                                                         ('ETF',          'ETF',      '指數股票型基金',   TRUE, TRUE, NOW(), NOW()),
                                                                                                         ('FUND',         '基金',     '共同基金',         TRUE, TRUE, NOW(), NOW()),
                                                                                                         ('BOND',         '債券',     '債券投資',         TRUE, TRUE, NOW(), NOW()),
                                                                                                         ('CRYPTO',       '加密貨幣', '加密貨幣資產',     TRUE, TRUE, NOW(), NOW()),
                                                                                                         ('REAL_ESTATE',  '不動產',   '不動產資產',       TRUE, TRUE, NOW(), NOW());


-- ========================================
-- TRANSACTION_TYPES
-- 設計重點：
--   系統主檔（非 tenant-aware）
--   CODE 直接當 PK
--   AMOUNT_SIGN：-1=支出 / 1=收入 / 0=轉帳
--   IS_TRANSFER / IS_ACTIVE 為常見查詢條件，各自加 INDEX
-- ========================================

CREATE TABLE transaction_types (
    -- PK
                                   CODE         VARCHAR(30)  NOT NULL,                      -- 交易類型代碼，如 EXPENSE

    -- 顯示資訊
                                   NAME         VARCHAR(50)  NOT NULL,                      -- 類型名稱，如 支出

    -- 行為定義
                                   AMOUNT_SIGN  TINYINT      NOT NULL,                      -- -1=支出 / 1=收入 / 0=轉帳
                                   IS_TRANSFER  BOOLEAN      NOT NULL,                      -- 是否帳戶間轉移
                                   AFFECT_ASSET BOOLEAN      NOT NULL,                      -- 是否影響資產總額

                                   DESCRIPTION  VARCHAR(255),                               -- 類型用途說明（Optional）

    -- 狀態
                                   IS_ACTIVE    BOOLEAN      NOT NULL,                      -- 是否可使用

    -- 稽核欄位
                                   CREATED_AT   DATETIME     NOT NULL,                      -- 建立時間
                                   UPDATED_AT   DATETIME     NOT NULL,                      -- 更新時間

                                   PRIMARY KEY (CODE),

    -- Index
                                   INDEX idx_tx_type_transfer          (IS_TRANSFER),
                                   INDEX idx_tx_type_active            (IS_ACTIVE)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO transaction_types (CODE, NAME, AMOUNT_SIGN, IS_TRANSFER, AFFECT_ASSET, DESCRIPTION, IS_ACTIVE, CREATED_AT, UPDATED_AT) VALUES
                                                                                                                                       ('INCOME',       '收入', 1,  FALSE, TRUE,  '一般收入，如薪資、獎金',   TRUE, NOW(), NOW()),
                                                                                                                                       ('EXPENSE',      '支出', -1, FALSE, TRUE,  '一般支出，如消費、帳單',   TRUE, NOW(), NOW()),
                                                                                                                                       ('TRANSFER_OUT', '轉出', -1, TRUE,  FALSE, '帳戶轉出',                 TRUE, NOW(), NOW()),
                                                                                                                                       ('TRANSFER_IN',  '轉入', 1,  TRUE,  FALSE, '帳戶轉入',                 TRUE, NOW(), NOW()),
                                                                                                                                       ('ADJUSTMENT',   '調整', 0,  FALSE, TRUE,  '餘額手動調整',             TRUE, NOW(), NOW());


-- ========================================
-- BANKS
-- 設計重點：
--   全域系統主檔，僅系統管理員可維護
--   BANK_CODE 由金管會定義，穩定不變，直接當 PK
--   BANK_BRANCHES 透過 BANK_CODE 做 FK，JOIN 直覺
-- ========================================

CREATE TABLE banks (
    -- PK
                       BANK_CODE  VARCHAR(10)  NOT NULL,                        -- 銀行識別碼，如 004

    -- 顯示資訊
                       NAME       VARCHAR(100) NOT NULL,                        -- 銀行全名
                       SHORT_NAME VARCHAR(50),                                  -- 顯示簡稱（Optional）
                       COUNTRY    VARCHAR(50)  NOT NULL,                        -- 所屬國家，如 TW

    -- 識別前綴
                       ACCOUNT_PREFIX VARCHAR(10),                               -- 帳號識別前綴，如永豐為 169（Optional）

    -- 狀態
                       IS_ACTIVE  BOOLEAN      NOT NULL,                        -- 是否可使用

    -- 稽核欄位
                       CREATED_AT DATETIME     NOT NULL,                        -- 建立時間
                       UPDATED_AT DATETIME     NOT NULL,                        -- 更新時間

                       PRIMARY KEY (BANK_CODE),

    -- Index
                       INDEX idx_bank_country              (COUNTRY),
                       INDEX idx_bank_active               (IS_ACTIVE)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO banks (BANK_CODE, NAME, SHORT_NAME, COUNTRY, ACCOUNT_PREFIX, IS_ACTIVE, CREATED_AT, UPDATED_AT) VALUES
                                                                                                                ('004', '臺灣銀行',         '台銀',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('005', '土地銀行',         '土銀',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('006', '合作金庫商業銀行', '合庫',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('007', '第一商業銀行',     '一銀',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('008', '華南商業銀行',     '華南',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('009', '彰化商業銀行',     '彰銀',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('011', '上海商業儲蓄銀行', '上海商銀', 'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('012', '台北富邦商業銀行', '北富銀',   'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('013', '國泰世華商業銀行', '國泰世華', 'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('017', '兆豐國際商業銀行', '兆豐',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('050', '臺灣企銀',         '台企銀',   'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('052', '渣打國際商業銀行', '渣打',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('700', '中華郵政',         '郵局',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('803', '聯邦商業銀行',     '聯邦',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('806', '元大商業銀行',     '元大',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('807', '永豐商業銀行',     '永豐',     'TW', '169', TRUE, NOW(), NOW()),
                                                                                                                ('808', '玉山商業銀行',     '玉山',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('809', '凱基商業銀行',     '凱基',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('812', '台新國際商業銀行', '台新',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('816', '安泰商業銀行',     '安泰',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('822', '中國信託商業銀行', '中信',     'TW', NULL,  TRUE, NOW(), NOW()),
                                                                                                                ('826', '樂天國際商業銀行', '樂天',     'TW', NULL,  TRUE, NOW(), NOW());

-- ========================================
-- BANK_FILE_FORMATS
-- 設計重點：
--   系統主檔（非 tenant-aware）
--   每間銀行、每種檔案類型、每個版本一筆記錄
--   同一 bank_code + file_type 取 released_date <= 上傳日期的最新一筆
--   parser_key 同銀行同 file_type 所有版本共用
-- ========================================

CREATE TABLE bank_file_formats (
    -- PK
                                   ID            BIGINT       NOT NULL AUTO_INCREMENT,

    -- 銀行關聯
                                   BANK_CODE     VARCHAR(10)  NOT NULL,                    -- 所屬銀行

    -- 格式資訊
                                   FILE_TYPE     VARCHAR(10)  NOT NULL,                    -- EXCEL / CSV
                                   RELEASED_DATE DATE         NOT NULL,                    -- 銀行格式釋出日期
                                   PARSER_KEY    VARCHAR(100) NOT NULL,                    -- 對應 parser 實作，格式：{BANK_CODE}_{FILE_TYPE}_{YYYYMMDD}，如 807_CSV_20250401

    -- 狀態
                                   IS_ACTIVE     TINYINT      NOT NULL DEFAULT 1,          -- 是否啟用

    -- 稽核欄位
                                   CREATED_AT    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   UPDATED_AT    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                   PRIMARY KEY (ID),
                                   UNIQUE KEY uk_parser_key (PARSER_KEY),
                                   INDEX idx_bank_file_formats_lookup (BANK_CODE, FILE_TYPE, RELEASED_DATE),

                                   CONSTRAINT fk_bff_bank
                                       FOREIGN KEY (BANK_CODE)
                                           REFERENCES banks(BANK_CODE)
                                           ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO bank_file_formats (BANK_CODE, FILE_TYPE, RELEASED_DATE, PARSER_KEY, IS_ACTIVE, CREATED_AT, UPDATED_AT) VALUES
    ('806', 'CSV', '2026-06-15', '806_CSV_20260615', 1, NOW(), NOW()),
    ('807', 'CSV', '2025-04-01', '807_CSV_20250401', 1, NOW(), NOW()),
    ('808', 'CSV', '2025-04-01', '808_CSV_20250401', 1, NOW(), NOW()),
    ('822', 'CSV', '2025-04-01', '822_CSV_20250401', 1, NOW(), NOW());



-- ========================================
-- BANK_BRANCHES
-- 設計重點：
--   與 BANKS 為 1:N 關係
--   同一銀行內 BRANCH_CODE 不可重複
--   UNIQUE (BANK_CODE, BRANCH_CODE)
--   BANK_CODE FK → BANKS(BANK_CODE) CASCADE
--   分行不可獨立存在，銀行刪除時分行一併刪除
-- ========================================

CREATE TABLE bank_branches (
    -- PK
                               ID          BIGINT       AUTO_INCREMENT PRIMARY KEY,     -- 主鍵

    -- 銀行關聯
                               BANK_CODE   VARCHAR(10)  NOT NULL,                       -- 所屬銀行代碼
                               BRANCH_CODE VARCHAR(10)  NOT NULL,                       -- 銀行內部分行代碼，如 0041001

    -- 顯示資訊
                               NAME        VARCHAR(100) NOT NULL,                       -- 分行名稱，如 台北分行
                               ADDRESS     VARCHAR(200),                                -- 分行地址（Optional）
                               PHONE       VARCHAR(30),                                 -- 分行電話（Optional）

    -- 狀態
                               IS_ACTIVE   BOOLEAN      NOT NULL,                       -- 是否啟用

    -- 稽核欄位
                               CREATED_AT  DATETIME     NOT NULL,                       -- 建立時間
                               UPDATED_AT  DATETIME     NOT NULL,                       -- 更新時間

    -- Unique：同銀行內分行代碼唯一
                               UNIQUE KEY uk_bank_branch           (BANK_CODE, BRANCH_CODE),

    -- Index
                               INDEX idx_bank_code                 (BANK_CODE),         -- 查詢某銀行所有分行
                               INDEX idx_branch_active             (IS_ACTIVE),         -- 篩選可用分行

    -- FK
                               CONSTRAINT fk_branch_bank
                                   FOREIGN KEY (BANK_CODE)
                                       REFERENCES banks(BANK_CODE)
                                       ON DELETE CASCADE                                -- 銀行刪除時分行一併刪除

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


SET FOREIGN_KEY_CHECKS = 1;