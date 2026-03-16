-- ========================================
-- NovaLedger Schema
-- Flyway Migration V6
-- Transactions：USER_TAGS、TRANSACTIONS、
--               TRANSACTION_ITEMS、TRANSACTION_TAGS
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- USER_TAGS
-- 設計重點：
--   tenant-aware
--   同租戶標籤名稱唯一
--   uk_tenant_tag (TENANT_ID, TAG_NAME) → 同租戶名稱唯一
--   idx_tenant_deleted → 查詢租戶有效標籤
--   idx_tenant_active_tag → 查詢啟用中標籤主路徑
-- ========================================

CREATE TABLE USER_TAGS (
    -- PK
                           ID         BIGINT      AUTO_INCREMENT PRIMARY KEY,       -- 主鍵

    -- 租戶 & 使用者
                           TENANT_ID  BIGINT      NOT NULL,                         -- 所屬帳本
                           USER_ID    BIGINT      NOT NULL,                         -- 建立者

    -- 識別
                           TAG_NAME   VARCHAR(50) NOT NULL,                         -- 使用者自訂名稱（同租戶唯一）
                           COLOR      VARCHAR(20),                                  -- UI 顯示用色碼（Optional）

    -- 狀態
                           IS_ACTIVE  BOOLEAN     NOT NULL,                         -- 標籤是否啟用

    -- 稽核欄位
                           CREATED_AT DATETIME    NOT NULL,                         -- 建立時間
                           UPDATED_AT DATETIME    NOT NULL,                         -- 更新時間
                           DELETED_AT DATETIME,                                     -- 軟刪除時間（Optional）

    -- Unique
                           UNIQUE KEY uk_tenant_tag            (TENANT_ID, TAG_NAME),

    -- Index
                           INDEX idx_tags_tenant_deleted       (TENANT_ID, DELETED_AT),          -- 查詢租戶有效標籤
                           INDEX idx_tags_tenant_active        (TENANT_ID, IS_ACTIVE, DELETED_AT), -- 查詢啟用中標籤
                           INDEX idx_tags_user                 (USER_ID),                        -- 查詢使用者建立的標籤

    -- FK
                           CONSTRAINT fk_tags_tenant
                               FOREIGN KEY (TENANT_ID)
                                   REFERENCES TENANTS(ID)
                                   ON DELETE CASCADE,

                           CONSTRAINT fk_tags_user
                               FOREIGN KEY (USER_ID)
                                   REFERENCES USERS(ID)
                                   ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ========================================
-- TRANSACTIONS
-- 設計重點：
--   tenant-aware，記帳系統核心表
--   ACCOUNT_ID 與 CREDIT_CARD_ID 二選一（CHECK constraint 保證）
--   TX_TYPE_CODE → TRANSACTION_TYPES(CODE)，與 V3 一致
--   軟刪除保留歷史
--   idx_tx_tenant_deleted → 全租戶交易查詢主路徑
--   idx_tx_account_date   → 帳戶交易 + 日期範圍查詢
--   idx_tx_cc_date        → 信用卡交易 + 日期範圍查詢
-- ========================================

CREATE TABLE TRANSACTIONS (
    -- PK
                              ID               BIGINT         AUTO_INCREMENT PRIMARY KEY,  -- 主鍵

    -- 租戶 & 使用者
                              TENANT_ID        BIGINT         NOT NULL,                    -- 所屬帳本
                              USER_ID          BIGINT         NOT NULL,                    -- 交易建立者

    -- 來源（二選一）
                              ACCOUNT_ID       BIGINT,                                     -- 所屬帳戶（與 CREDIT_CARD_ID 二選一）
                              CREDIT_CARD_ID   BIGINT,                                     -- 所屬信用卡（與 ACCOUNT_ID 二選一）

    -- 交易資訊
                              TX_TYPE_CODE     VARCHAR(30)    NOT NULL,                    -- 交易類型代碼 → TRANSACTION_TYPES
                              TRANSACTION_DATE DATE           NOT NULL,                    -- 交易發生日期
                              TOTAL_AMOUNT     DECIMAL(18, 2) NOT NULL,                    -- 交易總金額（正值）
                              CURRENCY_CODE    CHAR(3)        NOT NULL,                    -- ISO 4217 幣別
                              MEMO             VARCHAR(255),                               -- 使用者備註（Optional）

    -- 稽核欄位
                              CREATED_AT       DATETIME       NOT NULL,                    -- 建立時間
                              UPDATED_AT       DATETIME       NOT NULL,                    -- 更新時間
                              DELETED_AT       DATETIME,                                   -- 軟刪除時間（Optional）

    -- Check：ACCOUNT_ID 與 CREDIT_CARD_ID 二選一
                              CONSTRAINT chk_tx_source CHECK (
                                  (ACCOUNT_ID IS NOT NULL AND CREDIT_CARD_ID IS NULL) OR
                                  (ACCOUNT_ID IS NULL     AND CREDIT_CARD_ID IS NOT NULL)
                                  ),

    -- Index
                              INDEX idx_tx_tenant_deleted         (TENANT_ID, DELETED_AT),               -- 全租戶查詢主路徑
                              INDEX idx_tx_account_date           (TENANT_ID, ACCOUNT_ID, TRANSACTION_DATE),     -- 帳戶交易+日期
                              INDEX idx_tx_cc_date                (TENANT_ID, CREDIT_CARD_ID, TRANSACTION_DATE), -- 信用卡交易+日期
                              INDEX idx_tx_type                   (TX_TYPE_CODE),                        -- 按交易類型篩選
                              INDEX idx_tx_currency               (CURRENCY_CODE),                       -- 按幣別篩選
                              INDEX idx_tx_date                   (TRANSACTION_DATE),                    -- 日期範圍查詢

    -- FK
                              CONSTRAINT fk_tx_tenant
                                  FOREIGN KEY (TENANT_ID)
                                      REFERENCES TENANTS(ID)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_tx_user
                                  FOREIGN KEY (USER_ID)
                                      REFERENCES USERS(ID)
                                      ON DELETE RESTRICT,           -- 使用者刪除時不可連帶刪除交易記錄

                              CONSTRAINT fk_tx_account
                                  FOREIGN KEY (ACCOUNT_ID)
                                      REFERENCES USER_ACCOUNTS(ID)
                                      ON DELETE RESTRICT,           -- 帳戶有交易時不可刪除

                              CONSTRAINT fk_tx_credit_card
                                  FOREIGN KEY (CREDIT_CARD_ID)
                                      REFERENCES USER_CREDIT_CARDS(ID)
                                      ON DELETE RESTRICT,           -- 信用卡有交易時不可刪除

                              CONSTRAINT fk_tx_type
                                  FOREIGN KEY (TX_TYPE_CODE)
                                      REFERENCES TRANSACTION_TYPES(CODE)
                                      ON DELETE RESTRICT,

                              CONSTRAINT fk_tx_currency
                                  FOREIGN KEY (CURRENCY_CODE)
                                      REFERENCES CURRENCIES(CODE)
                                      ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ========================================
-- TRANSACTION_ITEMS
-- 設計重點：
--   支援一筆交易拆成多個明細
--   明細金額加總 = TRANSACTIONS.TOTAL_AMOUNT（應用層驗證）
--   CATEGORY_ID SET NULL：分類刪除不影響明細歷史記錄
--   補 CREATED_AT：財務核心明細，審計需要
--   idx_tenant_tx   → 查詢交易所有明細主路徑
--   idx_category    → 查詢分類下所有明細（報表用）
-- ========================================

CREATE TABLE TRANSACTION_ITEMS (
    -- PK
                                   ID             BIGINT         AUTO_INCREMENT PRIMARY KEY,  -- 主鍵

    -- 租戶 & 交易
                                   TENANT_ID      BIGINT         NOT NULL,                    -- 所屬帳本
                                   TRANSACTION_ID BIGINT         NOT NULL,                    -- 所屬交易

    -- 分類
                                   CATEGORY_ID    BIGINT,                                     -- 交易分類（Optional）

    -- 金額
                                   AMOUNT         DECIMAL(18, 2) NOT NULL,                    -- 明細金額

    -- 備註
                                   MEMO           VARCHAR(255),                               -- 明細說明（Optional）

    -- 稽核欄位（補強：財務核心明細需要審計時間戳）
                                   CREATED_AT     DATETIME       NOT NULL,                    -- 建立時間

    -- Index
                                   INDEX idx_ti_tenant_tx              (TENANT_ID, TRANSACTION_ID),  -- 查詢交易明細主路徑
                                   INDEX idx_ti_category               (TENANT_ID, CATEGORY_ID),     -- 分類報表查詢

    -- FK
                                   CONSTRAINT fk_ti_tenant
                                       FOREIGN KEY (TENANT_ID)
                                           REFERENCES TENANTS(ID)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_ti_transaction
                                       FOREIGN KEY (TRANSACTION_ID)
                                           REFERENCES TRANSACTIONS(ID)
                                           ON DELETE CASCADE,            -- 交易刪除時明細一併刪除

                                   CONSTRAINT fk_ti_category
                                       FOREIGN KEY (CATEGORY_ID)
                                           REFERENCES CATEGORIES(ID)
                                           ON DELETE SET NULL            -- 分類刪除時明細保留，分類欄位設 NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ========================================
-- TRANSACTION_TAGS
-- 設計重點：
--   交易與標籤的多對多關聯
--   僅做關聯，不放業務欄位
--   tenant-aware 防止跨帳本誤關聯
--   複合 FK (TENANT_ID, TRANSACTION_ID) MySQL 不支援，
--   拆成獨立 FK，應用層驗證 tenant 一致性
--   uk_tx_tag → 避免重複關聯
--   idx_tag_lookup → 支援標籤反查交易
-- ========================================

CREATE TABLE TRANSACTION_TAGS (
    -- PK
                                  ID             BIGINT   AUTO_INCREMENT PRIMARY KEY,        -- 主鍵

    -- 核心關聯
                                  TENANT_ID      BIGINT   NOT NULL,                          -- 所屬帳本
                                  TRANSACTION_ID BIGINT   NOT NULL,                          -- 對應交易
                                  TAG_ID         BIGINT   NOT NULL,                          -- 關聯標籤

    -- Unique：避免同筆交易重複掛同一標籤
                                  UNIQUE KEY uk_tx_tag                (TENANT_ID, TRANSACTION_ID, TAG_ID),

    -- Index
                                  INDEX idx_ttag_tag_lookup           (TENANT_ID, TAG_ID),   -- 標籤反查交易

    -- FK
                                  CONSTRAINT fk_ttag_tenant
                                      FOREIGN KEY (TENANT_ID)
                                          REFERENCES TENANTS(ID)
                                          ON DELETE CASCADE,

                                  CONSTRAINT fk_ttag_transaction
                                      FOREIGN KEY (TRANSACTION_ID)
                                          REFERENCES TRANSACTIONS(ID)
                                          ON DELETE CASCADE,            -- 交易刪除時標籤關聯一併刪除

                                  CONSTRAINT fk_ttag_tag
                                      FOREIGN KEY (TAG_ID)
                                          REFERENCES USER_TAGS(ID)
                                          ON DELETE CASCADE             -- 標籤刪除時關聯一併刪除

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


SET FOREIGN_KEY_CHECKS = 1;