-- ========================================
-- NovaLedger Schema
-- Flyway Migration V8
-- Snapshots & Exchange Rates：ACCOUNT_BALANCES、
--                             ASSET_SNAPSHOTS、EXCHANGE_RATES
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- ACCOUNT_BALANCES
-- 設計重點：
--   儲存某時間點的帳戶餘額快照，用於效能優化與歷史回溯
--   不即時計算，僅作快照（排程或交易後觸發）
--   uk_account_snapshot → 同帳戶同日期只有一筆快照
--   idx_ab_account_history → 查詢帳戶歷史餘額趨勢
-- ========================================

CREATE TABLE account_balances (
    -- PK
                                  ID            BIGINT         AUTO_INCREMENT PRIMARY KEY,   -- 主鍵

    -- 租戶 & 帳戶
                                  TENANT_ID     BIGINT         NOT NULL,                     -- 所屬帳本
                                  ACCOUNT_ID    BIGINT         NOT NULL,                     -- 對應帳戶

    -- 快照資訊
                                  SNAPSHOT_DATE DATE           NOT NULL,                     -- 餘額快照日期
                                  BALANCE       DECIMAL(18, 2) NOT NULL,                     -- 帳戶餘額
                                  CURRENCY_CODE CHAR(3)        NOT NULL,                     -- 幣別

    -- 稽核欄位
                                  CREATED_AT    DATETIME       NOT NULL,                     -- 建立時間
                                  UPDATED_AT    DATETIME       NOT NULL,                     -- 更新時間

    -- Unique：同帳戶同日期只有一筆快照
                                  UNIQUE KEY uk_account_snapshot      (TENANT_ID, ACCOUNT_ID, SNAPSHOT_DATE),

    -- Index
                                  INDEX idx_ab_account_history        (TENANT_ID, ACCOUNT_ID, SNAPSHOT_DATE), -- 帳戶餘額趨勢查詢

    -- FK
                                  CONSTRAINT fk_ab_tenant
                                      FOREIGN KEY (TENANT_ID)
                                          REFERENCES tenants(ID)
                                          ON DELETE CASCADE,

                                  CONSTRAINT fk_ab_account
                                      FOREIGN KEY (ACCOUNT_ID)
                                          REFERENCES user_accounts(ID)
                                          ON DELETE CASCADE,            -- 帳戶刪除時快照一併刪除

                                  CONSTRAINT fk_ab_currency
                                      FOREIGN KEY (CURRENCY_CODE)
                                          REFERENCES currencies(CODE)
                                          ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ========================================
-- ASSET_SNAPSHOTS
-- 設計重點：
--   儲存某時間點整體資產狀態的快照
--   供資產趨勢分析、歷史回溯、報表計算使用
--   不做刪除、不做修改（歷史快照資料）
--   NET_ASSET 用 Generated Column 自動計算，不需應用層維護
--   uk_asset_snapshots_daily → 同租戶同日期只有一筆快照
--   idx_as_trend → 資產趨勢查詢主路徑
-- ========================================

CREATE TABLE asset_snapshots (
    -- PK
                                 ID              BIGINT         AUTO_INCREMENT PRIMARY KEY,  -- 主鍵

    -- 租戶
                                 TENANT_ID       BIGINT         NOT NULL,                    -- 所屬帳本

    -- 快照日期
                                 SNAPSHOT_DATE   DATE           NOT NULL,                    -- 資產快照日

    -- 資產數據
                                 TOTAL_ASSET     DECIMAL(18, 2) NOT NULL,                    -- 所有資產總和
                                 TOTAL_LIABILITY DECIMAL(18, 2) NOT NULL,                    -- 所有負債總和

    -- Generated Column：自動計算淨資產，不需應用層維護
                                 NET_ASSET       DECIMAL(18, 2) GENERATED ALWAYS AS
                                     (TOTAL_ASSET - TOTAL_LIABILITY) STORED, -- 淨資產

    -- 稽核欄位
                                 CREATED_AT      DATETIME       NOT NULL,                    -- 建立時間
                                 UPDATED_AT      DATETIME       NOT NULL,                    -- 最後更新時間

    -- Unique：同租戶同日期只有一筆快照
                                 UNIQUE KEY uk_asset_snapshots_daily (TENANT_ID, SNAPSHOT_DATE),

    -- Index
                                 INDEX idx_as_trend                  (TENANT_ID, SNAPSHOT_DATE),  -- 資產趨勢查詢主路徑

    -- FK
                                 CONSTRAINT fk_as_tenant
                                     FOREIGN KEY (TENANT_ID)
                                         REFERENCES tenants(ID)
                                         ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ========================================
-- EXCHANGE_RATES
-- 設計重點：
--   全系統共用（非 tenant-aware）
--   同幣別組合 + 日期僅一筆
--   僅新增、不更新、不刪除（歷史匯率資料）
--   所以只有 CREATED_AT，沒有 UPDATED_AT
--   uk_exchange_rates_daily → 同幣別組合同日期唯一
--   idx_er_latest → 查詢最新匯率主路徑
--   fk_er_base_currency / fk_er_quote_currency → 確保幣別存在於系統
-- ========================================

CREATE TABLE exchange_rates (
    -- PK
                                ID             BIGINT         AUTO_INCREMENT PRIMARY KEY,  -- 主鍵

    -- 幣別組合
                                BASE_CURRENCY  CHAR(3)        NOT NULL,                    -- 基準幣別，如 USD
                                QUOTE_CURRENCY CHAR(3)        NOT NULL,                    -- 目標幣別，如 TWD

    -- 匯率資訊
                                RATE           DECIMAL(12, 6) NOT NULL,                    -- 兌換比例
                                RATE_DATE      DATE           NOT NULL,                    -- 匯率生效日
                                SOURCE         VARCHAR(50),                                -- 資料來源（Optional）

    -- 稽核欄位（僅新增不更新，所以只有 CREATED_AT）
                                CREATED_AT     DATETIME       NOT NULL,                    -- 建立時間

    -- Unique：同幣別組合同日期唯一
                                UNIQUE KEY uk_exchange_rates_daily  (BASE_CURRENCY, QUOTE_CURRENCY, RATE_DATE),

    -- Index
                                INDEX idx_er_latest                 (BASE_CURRENCY, QUOTE_CURRENCY, RATE_DATE),  -- 查詢最新匯率

    -- FK
                                CONSTRAINT fk_er_base_currency
                                    FOREIGN KEY (BASE_CURRENCY)
                                        REFERENCES currencies(CODE)
                                        ON DELETE RESTRICT,           -- 幣別仍有匯率記錄時不可刪除

                                CONSTRAINT fk_er_quote_currency
                                    FOREIGN KEY (QUOTE_CURRENCY)
                                        REFERENCES currencies(CODE)
                                        ON DELETE RESTRICT            -- 幣別仍有匯率記錄時不可刪除

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


SET FOREIGN_KEY_CHECKS = 1;
