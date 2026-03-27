-- ========================================
-- NovaLedger Schema
-- Flyway Migration V4
-- Subscription：SUBSCRIPTION_PLANS、TENANT_SUBSCRIPTIONS、USER_LIMITS
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- SUBSCRIPTION_PLANS
-- 設計重點：
--   TENANT_ID = NULL  → 系統預設方案（FREE / PRO / MAX）
--   TENANT_ID 非 NULL → 企業自訂方案（Phase 3+）
--   uk_tenant_plan (TENANT_ID, PLAN_CODE) → 同租戶下方案代碼唯一
--   idx_tenant_active → 查詢租戶可用方案主路徑
-- ========================================

CREATE TABLE subscription_plans (
    -- PK
                                    ID                   BIGINT         AUTO_INCREMENT PRIMARY KEY,  -- 方案主鍵

    -- 租戶關聯
                                    TENANT_ID            BIGINT,                                      -- NULL=系統方案；非NULL=企業自訂方案

    -- 識別
                                    PLAN_CODE            VARCHAR(50)    NOT NULL,                     -- 方案唯一識別碼，如 FREE / PRO / MAX
                                    NAME                 VARCHAR(100)   NOT NULL,                     -- 顯示用名稱

    -- 價格
                                    MONTHLY_PRICE        DECIMAL(10, 2) NOT NULL,                     -- 月費金額，0=免費

    -- 資源限制
                                    MAX_ACCOUNTS         INT            NOT NULL,                     -- 可建立帳戶上限
                                    MAX_CREDIT_CARDS     INT            NOT NULL,                     -- 可建立信用卡上限
                                    MAX_MEMBERS          INT            NOT NULL,                     -- 租戶成員數上限

    -- 功能開關
                                    ALLOW_FAMILY_SHARING BOOLEAN        NOT NULL,                     -- 是否可邀請成員共享
                                    ENABLE_INVESTMENT    BOOLEAN        NOT NULL,                     -- 是否可使用投資功能
                                    ENABLE_ADV_REPORT    BOOLEAN        NOT NULL,                     -- 是否可使用進階報表

    -- 狀態
                                    IS_ACTIVE            BOOLEAN        NOT NULL,                     -- 是否可使用

    -- 稽核欄位
                                    CREATED_AT           DATETIME       NOT NULL,                     -- 建立時間
                                    UPDATED_AT           DATETIME       NOT NULL,                     -- 更新時間

    -- Unique：同租戶下方案代碼唯一；系統方案 TENANT_ID = NULL
                                    UNIQUE KEY uk_tenant_plan           (TENANT_ID, PLAN_CODE),

    -- Index
                                    INDEX idx_sp_tenant_active          (TENANT_ID, IS_ACTIVE),      -- 查詢租戶可用方案

    -- FK
                                    CONSTRAINT fk_sp_tenant
                                        FOREIGN KEY (TENANT_ID)
                                            REFERENCES tenants(ID)
                                            ON DELETE CASCADE             -- 租戶刪除時，自訂方案一併刪除；系統方案不受影響

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed：系統預設方案
INSERT INTO subscription_plans
(TENANT_ID, PLAN_CODE, NAME, MONTHLY_PRICE,
 MAX_ACCOUNTS, MAX_CREDIT_CARDS, MAX_MEMBERS,
 ALLOW_FAMILY_SHARING, ENABLE_INVESTMENT, ENABLE_ADV_REPORT,
 IS_ACTIVE, CREATED_AT, UPDATED_AT)
VALUES
    (NULL, 'FREE', '免費方案', 0.00,    3,  3,  1, FALSE, FALSE, FALSE, TRUE, NOW(), NOW()),
    (NULL, 'PRO',  'PRO 方案', 99.00,  10, 10,  1, FALSE, TRUE,  TRUE,  TRUE, NOW(), NOW()),
    (NULL, 'MAX',  'MAX 方案', 199.00, 20, 20,  5, TRUE,  TRUE,  TRUE,  TRUE, NOW(), NOW());


-- ========================================
-- TENANT_SUBSCRIPTIONS
-- 設計重點：
--   記錄 tenant 目前使用的訂閱方案與狀態
--   同一 tenant 同時間僅一筆 STATUS = 'ACTIVE'
--   ACTIVE_KEY Generated Column + UNIQUE → DB 層 fail-fast 保證唯一性
--   過期訂閱保留歷史記錄（不做軟刪除，STATUS 區分狀態）
--   idx_ts_tenant_status → 查詢租戶目前方案主路徑
-- ========================================

CREATE TABLE tenant_subscriptions (
    -- PK
                                      ID         BIGINT      AUTO_INCREMENT PRIMARY KEY,       -- 主鍵

    -- 核心關聯
                                      TENANT_ID  BIGINT      NOT NULL,                         -- 所屬租戶
                                      PLAN_ID    BIGINT      NOT NULL,                         -- 使用中方案

    -- 狀態
                                      STATUS     VARCHAR(20) NOT NULL,                         -- ACTIVE / EXPIRED / CANCELLED
                                      IS_TRIAL   BOOLEAN     NOT NULL,                         -- 是否試用期

    -- 期間
                                      START_AT   DATETIME    NOT NULL,                         -- 訂閱起始時間
                                      END_AT     DATETIME,                                     -- 到期時間（Optional，NULL=無限期）

    -- Generated Column：STATUS = ACTIVE 時為 1，其餘為 NULL
    -- 搭配 UNIQUE KEY 保證同 tenant 只有一筆 ACTIVE
                                      ACTIVE_KEY TINYINT GENERATED ALWAYS AS (
                                          CASE WHEN STATUS = 'ACTIVE' THEN 1 ELSE NULL END
                                          ) STORED,

    -- 稽核欄位
                                      CREATED_AT DATETIME    NOT NULL,                         -- 建立時間
                                      UPDATED_AT DATETIME    NOT NULL,                         -- 更新時間

    -- Unique：DB 層保證同 tenant 只有一筆 ACTIVE
                                      UNIQUE KEY uk_ts_tenant_active      (TENANT_ID, ACTIVE_KEY),

    -- Index
                                      INDEX idx_ts_tenant_status          (TENANT_ID, STATUS), -- 查詢租戶訂閱狀態主路徑
                                      INDEX idx_ts_plan                   (PLAN_ID),           -- 查詢某方案有多少租戶使用
                                      INDEX idx_ts_end_at                 (END_AT),            -- 排程查詢即將到期訂閱

    -- FK
                                      CONSTRAINT fk_ts_tenant
                                          FOREIGN KEY (TENANT_ID)
                                              REFERENCES tenants(ID)
                                              ON DELETE CASCADE,

                                      CONSTRAINT fk_ts_plan
                                          FOREIGN KEY (PLAN_ID)
                                              REFERENCES subscription_plans(ID)
                                              ON DELETE RESTRICT            -- 方案仍有租戶使用時不可刪除

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ========================================
-- USER_LIMITS
-- 設計重點：
--   追蹤 tenant 層級的實際資源使用量
--   與方案上限即時比對（應用層負責比對邏輯）
--   uk_tenant_resource → DB 層保證一資源類型一筆
--   補 CREATED_AT：追蹤資源開始被計算的時間點，便於 debug 與審計
-- ========================================

CREATE TABLE user_limits (
    -- PK
                             ID            BIGINT      AUTO_INCREMENT PRIMARY KEY,    -- 主鍵

    -- 核心關聯
                             TENANT_ID     BIGINT      NOT NULL,                      -- 所屬租戶

    -- 資源追蹤
                             RESOURCE_TYPE VARCHAR(50) NOT NULL,                      -- 資源類型，如 ACCOUNT / CREDIT_CARD
                             USED_COUNT    INT         NOT NULL,                      -- 目前使用量

    -- 稽核欄位
                             CREATED_AT    DATETIME    NOT NULL,                      -- 建立時間（資源開始被追蹤的時間點）
                             UPDATED_AT    DATETIME    NOT NULL,                      -- 最後同步時間

    -- Unique：同租戶下一種資源類型只有一筆
                             UNIQUE KEY uk_tenant_resource       (TENANT_ID, RESOURCE_TYPE),

    -- Index
                             INDEX idx_ul_tenant                 (TENANT_ID),         -- 查詢租戶所有資源使用量

    -- FK
                             CONSTRAINT fk_ul_tenant
                                 FOREIGN KEY (TENANT_ID)
                                     REFERENCES tenants(ID)
                                     ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


SET FOREIGN_KEY_CHECKS = 1;