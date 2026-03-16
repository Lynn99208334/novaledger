-- ========================================
-- NovaLedger Schema
-- Flyway Migration V7
-- Budgets & Recurring：USER_BUDGETS、RECURRING_TRANSACTIONS
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- USER_BUDGETS
-- 設計重點：
--   tenant-aware，以「期間」為單位設定預算
--   CATEGORY_ID = NULL → 全體預算（不限分類）
--   uk_budget_period (TENANT_ID, CATEGORY_ID, PERIOD) → 同租戶同分類同期間唯一
--   idx_period_active → 查詢租戶當期有效預算主路徑
--   CURRENT_SPENT 即時更新（交易寫入後同步）
--   複合 FK (TENANT_ID, CATEGORY_ID) MySQL 不支援，拆成獨立 FK
-- ========================================

CREATE TABLE USER_BUDGETS (
    -- PK
                              ID            BIGINT         AUTO_INCREMENT PRIMARY KEY,   -- 主鍵

    -- 租戶 & 使用者
                              TENANT_ID     BIGINT         NOT NULL,                     -- 所屬帳本
                              USER_ID       BIGINT         NOT NULL,                     -- 建立者

    -- 預算範圍
                              CATEGORY_ID   BIGINT,                                      -- 預算分類（Optional，NULL=全體預算）
                              PERIOD        CHAR(7)        NOT NULL,                     -- 預算期間，格式 YYYY-MM

    -- 金額
                              BUDGET_AMOUNT DECIMAL(18, 2) NOT NULL,                     -- 預算上限
                              CURRENT_SPENT DECIMAL(18, 2) NOT NULL DEFAULT 0,           -- 已使用金額（即時更新）

    -- 狀態
                              IS_ACTIVE     BOOLEAN        NOT NULL,                     -- 是否啟用預算

    -- 稽核欄位
                              CREATED_AT    DATETIME       NOT NULL,                     -- 建立時間
                              UPDATED_AT    DATETIME       NOT NULL,                     -- 更新時間
                              DELETED_AT    DATETIME,                                    -- 軟刪除時間（Optional）

    -- Unique：同租戶同分類同期間只能有一筆預算
                              UNIQUE KEY uk_budget_period         (TENANT_ID, CATEGORY_ID, PERIOD),

    -- Index
                              INDEX idx_period_active             (TENANT_ID, PERIOD, IS_ACTIVE),   -- 查詢當期有效預算主路徑
                              INDEX idx_budgets_user              (USER_ID),                        -- 查詢使用者建立的預算
                              INDEX idx_budgets_category          (CATEGORY_ID),                    -- 分類預算查詢
                              INDEX idx_budgets_deleted           (TENANT_ID, DELETED_AT),          -- 軟刪除過濾

    -- FK
                              CONSTRAINT fk_bud_tenant
                                  FOREIGN KEY (TENANT_ID)
                                      REFERENCES TENANTS(ID)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_bud_user
                                  FOREIGN KEY (USER_ID)
                                      REFERENCES USERS(ID)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_bud_category
                                  FOREIGN KEY (CATEGORY_ID)
                                      REFERENCES CATEGORIES(ID)
                                      ON DELETE SET NULL            -- 分類刪除時預算保留，分類欄位設 NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ========================================
-- RECURRING_TRANSACTIONS
-- 設計重點：
--   tenant-aware，定義固定週期產生的交易模板
--   實際交易仍寫入 TRANSACTIONS（應用層排程觸發）
--   ACCOUNT_ID 與 CREDIT_CARD_ID 二選一（CHECK constraint）
--   NEXT_RUN_DATE 為排程任務查詢的關鍵欄位
--   idx_tenant_active → 查詢租戶啟用中模板
--   idx_next_run → 排程任務查詢「今天需要執行的模板」
--   複合 FK (TENANT_ID, CATEGORY_ID) MySQL 不支援，拆成獨立 FK
-- ========================================

CREATE TABLE RECURRING_TRANSACTIONS (
    -- PK
                                        ID             BIGINT         AUTO_INCREMENT PRIMARY KEY,  -- 主鍵

    -- 租戶 & 使用者
                                        TENANT_ID      BIGINT         NOT NULL,                    -- 所屬帳本
                                        USER_ID        BIGINT         NOT NULL,                    -- 建立者

    -- 來源（二選一）
                                        ACCOUNT_ID     BIGINT,                                     -- 使用帳戶（與 CREDIT_CARD_ID 二選一）
                                        CREDIT_CARD_ID BIGINT,                                     -- 使用信用卡（與 ACCOUNT_ID 二選一）

    -- 週期設定
                                        FREQUENCY      VARCHAR(20)    NOT NULL,                    -- 執行週期：MONTHLY / WEEKLY / YEARLY

    -- 交易資訊
                                        AMOUNT         DECIMAL(18, 2) NOT NULL,                    -- 固定金額
                                        TX_TYPE        VARCHAR(30)    NOT NULL,                    -- 交易類型：INCOME / EXPENSE
                                        CATEGORY_ID    BIGINT,                                     -- 對應分類（Optional）
                                        MEMO           VARCHAR(255),                               -- 說明（Optional）

    -- 期間設定
                                        START_DATE     DATE           NOT NULL,                    -- 開始日期
                                        END_DATE       DATE,                                       -- 結束日期（Optional，NULL=無限期）
                                        NEXT_RUN_DATE  DATE           NOT NULL,                    -- 下次排程執行日

    -- 狀態
                                        IS_ACTIVE      BOOLEAN        NOT NULL,                    -- 是否啟用

    -- 稽核欄位
                                        CREATED_AT     DATETIME       NOT NULL,                    -- 建立時間
                                        UPDATED_AT     DATETIME       NOT NULL,                    -- 更新時間
                                        DELETED_AT     DATETIME,                                   -- 軟刪除時間（Optional）

    -- Check：ACCOUNT_ID 與 CREDIT_CARD_ID 二選一
                                        CONSTRAINT chk_rec_source CHECK (
                                            (ACCOUNT_ID IS NOT NULL AND CREDIT_CARD_ID IS NULL) OR
                                            (ACCOUNT_ID IS NULL     AND CREDIT_CARD_ID IS NOT NULL)
                                            ),

    -- Index
                                        INDEX idx_rec_tenant_active         (TENANT_ID, IS_ACTIVE, DELETED_AT),  -- 查詢租戶啟用中模板
                                        INDEX idx_rec_next_run              (IS_ACTIVE, NEXT_RUN_DATE),           -- 排程任務查詢今日需執行模板
                                        INDEX idx_rec_user                  (USER_ID),                            -- 查詢使用者建立的模板
                                        INDEX idx_rec_frequency             (FREQUENCY),                          -- 按週期類型篩選
                                        INDEX idx_rec_category              (CATEGORY_ID),                        -- 分類查詢

    -- FK
                                        CONSTRAINT fk_rec_tenant
                                            FOREIGN KEY (TENANT_ID)
                                                REFERENCES TENANTS(ID)
                                                ON DELETE CASCADE,

                                        CONSTRAINT fk_rec_user
                                            FOREIGN KEY (USER_ID)
                                                REFERENCES USERS(ID)
                                                ON DELETE RESTRICT,           -- 使用者刪除時不可連帶刪除模板

                                        CONSTRAINT fk_rec_account
                                            FOREIGN KEY (ACCOUNT_ID)
                                                REFERENCES USER_ACCOUNTS(ID)
                                                ON DELETE CASCADE,            -- 帳戶刪除時週期模板一併刪除

                                        CONSTRAINT fk_rec_credit_card
                                            FOREIGN KEY (CREDIT_CARD_ID)
                                                REFERENCES USER_CREDIT_CARDS(ID)
                                                ON DELETE CASCADE,            -- 信用卡刪除時週期模板一併刪除

                                        CONSTRAINT fk_rec_category
                                            FOREIGN KEY (CATEGORY_ID)
                                                REFERENCES CATEGORIES(ID)
                                                ON DELETE SET NULL            -- 分類刪除時模板保留，分類欄位設 NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


SET FOREIGN_KEY_CHECKS = 1;