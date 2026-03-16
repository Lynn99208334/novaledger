-- ========================================
-- NovaLedger Schema
-- Flyway Migration V10
-- Reports & Scheduled Tasks：REPORT_DEFINITIONS、
--                            REPORT_RESULTS、SCHEDULED_TASKS
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- REPORT_DEFINITIONS
-- 設計重點：
--   定義「報表是什麼」，不存任何結果資料
--   TENANT_ID = NULL  → 系統預設報表
--   TENANT_ID 非 NULL → 租戶自訂報表
--   uk_report_definitions (TENANT_ID, REPORT_CODE) → 同租戶報表代碼唯一
--   idx_active_reports → 查詢租戶可用報表主路徑
--   DELETED_AT 保險用，不作主要查詢條件，單獨 INDEX
-- ========================================

CREATE TABLE REPORT_DEFINITIONS (
    -- PK
                                    ID          BIGINT       AUTO_INCREMENT PRIMARY KEY,       -- 主鍵

    -- 租戶關聯
                                    TENANT_ID   BIGINT,                                        -- NULL=系統預設報表；非NULL=租戶自訂

    -- 識別
                                    REPORT_CODE VARCHAR(50)  NOT NULL,                         -- 報表識別碼（同租戶唯一）
                                    REPORT_NAME VARCHAR(100) NOT NULL,                         -- 報表名稱
                                    DESCRIPTION VARCHAR(500),                                  -- 報表用途說明（Optional）

    -- 報表屬性
                                    REPORT_TYPE VARCHAR(30)  NOT NULL,                         -- ASSET / CASHFLOW / CATEGORY
                                    TIME_GRAIN  VARCHAR(20)  NOT NULL,                         -- DAY / MONTH / YEAR

    -- 狀態
                                    IS_ACTIVE   BOOLEAN      NOT NULL,                         -- 是否啟用

    -- 稽核欄位
                                    CREATED_AT  DATETIME     NOT NULL,                         -- 建立時間
                                    UPDATED_AT  DATETIME     NOT NULL,                         -- 最後更新時間
                                    DELETED_AT  DATETIME,                                      -- 軟刪除時間（保險用）

    -- Unique：同租戶下報表代碼唯一；系統報表 TENANT_ID = NULL
                                    UNIQUE KEY uk_report_definitions    (TENANT_ID, REPORT_CODE),

    -- Index
                                    INDEX idx_active_reports            (TENANT_ID, IS_ACTIVE, REPORT_TYPE), -- 查詢可用報表主路徑
                                    INDEX idx_rd_deleted_at             (DELETED_AT),                        -- 軟刪除過濾（保險用）

    -- FK
                                    CONSTRAINT fk_rd_tenant
                                        FOREIGN KEY (TENANT_ID)
                                            REFERENCES TENANTS(ID)
                                            ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed：系統預設報表
INSERT INTO REPORT_DEFINITIONS
(TENANT_ID, REPORT_CODE, REPORT_NAME, REPORT_TYPE, TIME_GRAIN, IS_ACTIVE, CREATED_AT, UPDATED_AT)
VALUES
    (NULL, 'MONTHLY_CASHFLOW',    '月度收支報表',   'CASHFLOW', 'MONTH', TRUE, NOW(), NOW()),
    (NULL, 'MONTHLY_CATEGORY',    '月度分類報表',   'CATEGORY', 'MONTH', TRUE, NOW(), NOW()),
    (NULL, 'ASSET_TREND_MONTHLY', '資產趨勢（月）', 'ASSET',    'MONTH', TRUE, NOW(), NOW()),
    (NULL, 'ASSET_TREND_YEARLY',  '資產趨勢（年）', 'ASSET',    'YEAR',  TRUE, NOW(), NOW()),
    (NULL, 'YEARLY_SUMMARY',      '年度總結報表',   'CASHFLOW', 'YEAR',  TRUE, NOW(), NOW());


-- ========================================
-- REPORT_RESULTS
-- 設計重點：
--   儲存某次報表執行後的結果快照
--   不做軟刪除（REPORT_DEFINITIONS 刪除時 CASCADE）
--   RESULT_DATA 為 JSON 型態，不加 INDEX
--   idx_rr_report   → 查詢某報表最新結果
--   idx_rr_period   → 查詢某時段內的歷史報表
--   idx_rr_latest   → 快速取得最新一次結果
-- ========================================

CREATE TABLE REPORT_RESULTS (
    -- PK
                                ID           BIGINT         AUTO_INCREMENT PRIMARY KEY,    -- 主鍵

    -- 租戶 & 報表
                                TENANT_ID    BIGINT         NOT NULL,                      -- 所屬租戶
                                REPORT_ID    BIGINT         NOT NULL,                      -- 對應報表定義

    -- 期間
                                START_DATE   DATE           NOT NULL,                      -- 報表區間起
                                END_DATE     DATE           NOT NULL,                      -- 報表區間迄

    -- 結果
                                RESULT_DATA  JSON           NOT NULL,                      -- 報表結果資料（JSON）

    -- 時間
                                GENERATED_AT DATETIME       NOT NULL,                      -- 報表產生時間

    -- Index
                                INDEX idx_rr_report                 (TENANT_ID, REPORT_ID),              -- 查詢某報表所有結果
                                INDEX idx_rr_period                 (TENANT_ID, START_DATE, END_DATE),   -- 查詢某時段歷史報表
                                INDEX idx_rr_latest                 (TENANT_ID, REPORT_ID, GENERATED_AT),-- 快速取得最新結果

    -- FK
                                CONSTRAINT fk_rr_tenant
                                    FOREIGN KEY (TENANT_ID)
                                        REFERENCES TENANTS(ID)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_rr_report
                                    FOREIGN KEY (REPORT_ID)
                                        REFERENCES REPORT_DEFINITIONS(ID)
                                        ON DELETE CASCADE             -- 報表定義刪除時，結果一併刪除

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ========================================
-- SCHEDULED_TASKS
-- 設計重點：
--   tenant-aware，定義租戶層級的排程任務設定
--   僅設定層，不記錄執行歷史（執行歷史由 IMPORT_LOGS 負責）
--   LAST_RUN_AT 用於檢查卡住的任務
--   idx_scheduled_tasks_active → 查詢租戶啟用中任務主路徑
--   idx_st_last_run → 排程監控查詢（檢查長時間未執行的任務）
-- ========================================

CREATE TABLE SCHEDULED_TASKS (
    -- PK
                                 ID          BIGINT       AUTO_INCREMENT PRIMARY KEY,       -- 主鍵

    -- 租戶
                                 TENANT_ID   BIGINT       NOT NULL,                         -- 所屬帳本

    -- 任務設定
                                 TASK_NAME   VARCHAR(100) NOT NULL,                         -- 任務名稱
                                 TASK_TYPE   VARCHAR(50)  NOT NULL,                         -- 任務用途，如 IMPORT / REPORT
                                 CRON_EXPR   VARCHAR(100) NOT NULL,                         -- Cron 排程表達式

    -- 狀態
                                 IS_ACTIVE   BOOLEAN      NOT NULL,                         -- 是否啟用

    -- 稽核欄位
                                 CREATED_AT  DATETIME     NOT NULL,                         -- 建立時間
                                 LAST_RUN_AT DATETIME,                                      -- 最後執行時間（Optional）

    -- Index
                                 INDEX idx_scheduled_tasks_active    (TENANT_ID, IS_ACTIVE, TASK_TYPE),  -- 查詢啟用中任務主路徑
                                 INDEX idx_st_last_run               (IS_ACTIVE, LAST_RUN_AT),           -- 檢查卡住任務

    -- FK
                                 CONSTRAINT fk_st_tenant
                                     FOREIGN KEY (TENANT_ID)
                                         REFERENCES TENANTS(ID)
                                         ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


SET FOREIGN_KEY_CHECKS = 1;