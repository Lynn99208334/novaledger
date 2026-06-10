-- ========================================
-- NovaLedger Schema
-- Flyway Migration V5
-- Accounts & Assets：CATEGORIES、INVESTMENT_PRODUCTS、
--                    USER_ACCOUNTS、USER_CREDIT_CARDS
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- CATEGORIES
-- 設計重點：
--   tenant-aware 表
--   支援階層結構（PARENT_ID）
--   同租戶下類別名稱不可重複
--   PARENT_ID FK 單一 FK + 應用層驗證跨租戶（MySQL 不支援複合 FK）
--   父類別刪除時，子類別提升為頂層（SET NULL）
--   uk_tenant_category (TENANT_ID, NAME) → 同租戶名稱唯一
--   idx_tenant_type_active → 查詢租戶有效收入/支出類別主路徑
-- ========================================

CREATE TABLE categories (
    -- PK
                            ID                 BIGINT      AUTO_INCREMENT PRIMARY KEY,   -- 類別主鍵

    -- 租戶關聯
                            TENANT_ID          BIGINT      NOT NULL,                     -- 所屬租戶

    -- 階層
                            PARENT_ID          BIGINT,                                   -- 上層分類（Optional，NULL=頂層）

    -- 識別
                            NAME               VARCHAR(50) NOT NULL,                     -- 類別名稱（同租戶唯一）
                            TYPE               VARCHAR(20) NOT NULL,                     -- INCOME / EXPENSE

    -- UI 顯示
                            ICON_NAME          VARCHAR(50),                              -- 圖示名稱（Optional）
                            COLOR_HEX          VARCHAR(7),                               -- 顏色代碼，#RRGGBB（Optional）

    -- 系統模板
                            IS_SYSTEM_DEFAULT  BOOLEAN     NOT NULL,                     -- 是否源自系統模板
                            SOURCE_TEMPLATE_ID BIGINT,                                   -- 原始模板 ID（Optional）

    -- 排序與狀態
                            SORT_ORDER         INT         NOT NULL DEFAULT 0,           -- 同層排序
                            IS_ACTIVE          BOOLEAN     NOT NULL,                     -- 是否可使用

    -- 稽核欄位
                            CREATED_AT         DATETIME    NOT NULL,                     -- 建立時間
                            UPDATED_AT         DATETIME    NOT NULL,                     -- 更新時間

    -- Unique：同租戶下類別名稱唯一
                            UNIQUE KEY uk_tenant_category       (TENANT_ID, NAME),

    -- Index
                            INDEX idx_tenant_type_active        (TENANT_ID, TYPE, IS_ACTIVE),  -- 查詢有效類別主路徑
                            INDEX idx_categories_parent         (PARENT_ID),                   -- 查詢子類別
                            INDEX idx_categories_template       (SOURCE_TEMPLATE_ID),          -- 查詢模板來源

    -- FK
                            CONSTRAINT fk_categories_tenant
                                FOREIGN KEY (TENANT_ID)
                                    REFERENCES tenants(ID)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_categories_parent
                                FOREIGN KEY (PARENT_ID)
                                    REFERENCES categories(ID)
                                    ON DELETE SET NULL            -- 父類別刪除時，子類別提升為頂層

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ========================================
-- INVESTMENT_PRODUCTS
-- 設計重點：
--   TENANT_ID = NULL  → 系統預設產品
--   TENANT_ID 非 NULL → 企業自訂產品（Phase 4+）
--   uk_tenant_product (TENANT_ID, CODE) → 同租戶下產品代碼唯一
--   idx_ip_tenant_active → 查詢可用投資產品主路徑
-- ========================================

CREATE TABLE investment_products (
    -- PK
                                     ID         BIGINT       AUTO_INCREMENT PRIMARY KEY,      -- 產品主鍵

    -- 租戶關聯
                                     TENANT_ID  BIGINT,                                       -- NULL=系統產品；非NULL=自訂產品

    -- 識別
                                     CODE       VARCHAR(50)  NOT NULL,                        -- 投資產品代碼，如 STOCK / ETF
                                     NAME       VARCHAR(100) NOT NULL,                        -- 產品名稱
                                     TYPE       VARCHAR(50)  NOT NULL,                        -- 產品類型，如 股票 / 債券
                                     DESCRIPTION VARCHAR(255),                                -- 產品說明（補強：前端顯示用）

    -- 狀態
                                     IS_ACTIVE  BOOLEAN      NOT NULL,                        -- 是否可選用

    -- 稽核欄位
                                     CREATED_AT DATETIME     NOT NULL,                        -- 建立時間
                                     UPDATED_AT DATETIME     NOT NULL,                        -- 更新時間

    -- Unique：同租戶下產品代碼唯一
                                     UNIQUE KEY uk_tenant_product        (TENANT_ID, CODE),

    -- Index
                                     INDEX idx_ip_tenant_active          (TENANT_ID, IS_ACTIVE),  -- 查詢可用投資產品
                                     INDEX idx_ip_type                   (TYPE),                  -- 按產品類型篩選

    -- FK
                                     CONSTRAINT fk_ip_tenant
                                         FOREIGN KEY (TENANT_ID)
                                             REFERENCES tenants(ID)
                                             ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO investment_products (TENANT_ID, CODE, NAME, TYPE, DESCRIPTION, IS_ACTIVE, CREATED_AT, UPDATED_AT) VALUES
                                                                                                                  (NULL, 'STOCK_TW',   '台股',     '股票',     '台灣上市上櫃股票',     TRUE, NOW(), NOW()),
                                                                                                                  (NULL, 'STOCK_US',   '美股',     '股票',     '美國上市股票',         TRUE, NOW(), NOW()),
                                                                                                                  (NULL, 'ETF_TW',     '台灣 ETF', 'ETF',      '台灣指數股票型基金',   TRUE, NOW(), NOW()),
                                                                                                                  (NULL, 'ETF_US',     '美國 ETF', 'ETF',      '美國指數股票型基金',   TRUE, NOW(), NOW()),
                                                                                                                  (NULL, 'FUND_MF',    '共同基金', '基金',     '共同基金投資',         TRUE, NOW(), NOW()),
                                                                                                                  (NULL, 'BOND_GOV',   '政府公債', '債券',     '政府發行公債',         TRUE, NOW(), NOW()),
                                                                                                                  (NULL, 'CRYPTO_BTC', '比特幣',   '加密貨幣', 'Bitcoin',              TRUE, NOW(), NOW()),
                                                                                                                  (NULL, 'CRYPTO_ETH', '以太幣',   '加密貨幣', 'Ethereum',             TRUE, NOW(), NOW());


-- ========================================
-- USER_ACCOUNTS
-- 設計重點：
--   tenant-aware，支援多種帳戶類型
--   BANK_CODE → BANKS(BANK_CODE) ON DELETE RESTRICT
--   BRANCH_ID → BANK_BRANCHES(ID) ON DELETE SET NULL
--   複合 FK 限制由應用層驗證（分行必須屬於該銀行）
--   CURRENT_BALANCE 交易後即時更新
--   軟刪除保留歷史
--   idx_tenant_user_active → 查詢使用者有效帳戶主路徑
-- ========================================

CREATE TABLE user_accounts (
    -- PK
                               ID              BIGINT         AUTO_INCREMENT PRIMARY KEY,  -- 主鍵

    -- 租戶 & 使用者
                               TENANT_ID       BIGINT         NOT NULL,                    -- 所屬租戶
                               USER_ID         BIGINT         NOT NULL,                    -- 建立者

    -- 帳戶類型
                               ACCOUNT_TYPE    VARCHAR(20)    NOT NULL,                    -- BANK / INVESTMENT / CASH

    -- 銀行資訊（BANK 類型必填）
                               BANK_CODE       VARCHAR(10),                                -- 所屬銀行代碼（Optional）
                               BRANCH_ID       BIGINT,                                     -- 所屬分行 ID（Optional）
                               ACCOUNT_NUMBER  VARCHAR(500),                               -- 帳戶號碼（AES-256-GCM 加密後 Base64，Optional）

    -- 顯示資訊
                               NAME            VARCHAR(100)   NOT NULL,                    -- 顯示名稱

    -- 金融資訊
                               CURRENCY_CODE   CHAR(3)        NOT NULL,                    -- 幣別
                               CURRENT_BALANCE DECIMAL(18, 2) NOT NULL,                    -- 當前餘額（交易後即時更新）
                               INITIAL_BALANCE DECIMAL(18, 2) NOT NULL,                    -- 初始餘額（建立時設定）

    -- 備註
                               NOTES           VARCHAR(500),                               -- 備註（Optional）

    -- 稽核欄位
                               CREATED_AT      DATETIME       NOT NULL,                    -- 建立時間
                               UPDATED_AT      DATETIME       NOT NULL,                    -- 更新時間
                               DELETED_AT      DATETIME,                                   -- 軟刪除時間（Optional）

    -- Index
                               INDEX idx_tenant_user_active        (TENANT_ID, USER_ID, DELETED_AT),  -- 查詢有效帳戶主路徑
                               INDEX idx_bank_branch               (BANK_CODE, BRANCH_ID),            -- 銀行分行查詢
                               INDEX idx_account_type              (ACCOUNT_TYPE),                    -- 按帳戶類型篩選
                               INDEX idx_ua_currency               (CURRENCY_CODE),                   -- 按幣別篩選

    -- FK
                               CONSTRAINT fk_accounts_tenant
                                   FOREIGN KEY (TENANT_ID)
                                       REFERENCES tenants(ID)
                                       ON DELETE CASCADE,

                               CONSTRAINT fk_accounts_user
                                   FOREIGN KEY (USER_ID)
                                       REFERENCES users(ID)
                                       ON DELETE CASCADE,

                               CONSTRAINT fk_accounts_bank
                                   FOREIGN KEY (BANK_CODE)
                                       REFERENCES banks(BANK_CODE)
                                       ON DELETE RESTRICT,           -- 銀行有帳戶時不可刪除

                               CONSTRAINT fk_accounts_branch
                                   FOREIGN KEY (BRANCH_ID)
                                       REFERENCES bank_branches(ID)
                                       ON DELETE SET NULL,           -- 分行關閉時帳戶保留，分行資訊清空

                               CONSTRAINT fk_accounts_currency
                                   FOREIGN KEY (CURRENCY_CODE)
                                       REFERENCES currencies(CODE)
                                       ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ========================================
-- USER_CREDIT_CARDS
-- 設計重點：
--   tenant-aware
--   CURRENT_BALANCE 追蹤本期消費（交易後更新）
--   BILLING_DATE / PAYMENT_DATE CHECK constraint 限制 1-31
--   軟刪除保留歷史
--   idx_tenant_user_active → 查詢使用者有效信用卡主路徑
-- ========================================

CREATE TABLE user_credit_cards (
    -- PK
                                   ID                BIGINT         AUTO_INCREMENT PRIMARY KEY,  -- 主鍵

    -- 租戶 & 使用者
                                   TENANT_ID         BIGINT         NOT NULL,                    -- 所屬租戶
                                   USER_ID           BIGINT         NOT NULL,                    -- 持卡人

    -- 銀行資訊
                                   BANK_CODE         VARCHAR(10)    NOT NULL,                    -- 發卡銀行

    -- 卡片資訊
                                   CARD_NUMBER_LAST4 VARCHAR(4),                                 -- 卡號末四碼（Optional）
                                   CARD_TYPE         VARCHAR(20),                                -- VISA / MASTERCARD / JCB（Optional）
                                   NAME              VARCHAR(100)   NOT NULL,                    -- 顯示名稱

    -- 金融資訊
                                   CURRENCY_CODE     CHAR(3)        NOT NULL,                    -- 幣別
                                   BILLING_DATE      TINYINT,                                    -- 帳單日 1-31（Optional）
                                   PAYMENT_DATE      TINYINT,                                    -- 繳款日 1-31（Optional）
                                   CREDIT_LIMIT      DECIMAL(18, 2),                             -- 信用額度（Optional）
                                   CURRENT_BALANCE   DECIMAL(18, 2) NOT NULL,                    -- 本期消費（交易後更新）

    -- 備註
                                   NOTES             VARCHAR(500),                               -- 備註（Optional）

    -- 稽核欄位
                                   CREATED_AT        DATETIME       NOT NULL,                    -- 建立時間
                                   UPDATED_AT        DATETIME       NOT NULL,                    -- 更新時間
                                   DELETED_AT        DATETIME,                                   -- 軟刪除時間（Optional）

    -- Check constraints
                                   CONSTRAINT chk_billing_date
                                       CHECK (BILLING_DATE IS NULL OR BILLING_DATE BETWEEN 1 AND 31),
                                   CONSTRAINT chk_payment_date
                                       CHECK (PAYMENT_DATE IS NULL OR PAYMENT_DATE BETWEEN 1 AND 31),

    -- Index
                                   INDEX idx_cc_tenant_user_active     (TENANT_ID, USER_ID, DELETED_AT),  -- 查詢有效信用卡主路徑
                                   INDEX idx_cc_bank                   (BANK_CODE),                       -- 按銀行篩選
                                   INDEX idx_cc_currency               (CURRENCY_CODE),                   -- 按幣別篩選

    -- FK
                                   CONSTRAINT fk_cc_tenant
                                       FOREIGN KEY (TENANT_ID)
                                           REFERENCES tenants(ID)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_cc_user
                                       FOREIGN KEY (USER_ID)
                                           REFERENCES users(ID)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_cc_bank
                                       FOREIGN KEY (BANK_CODE)
                                           REFERENCES banks(BANK_CODE)
                                           ON DELETE RESTRICT,           -- 銀行有信用卡時不可刪除

                                   CONSTRAINT fk_cc_currency
                                       FOREIGN KEY (CURRENCY_CODE)
                                           REFERENCES currencies(CODE)
                                           ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


SET FOREIGN_KEY_CHECKS = 1;