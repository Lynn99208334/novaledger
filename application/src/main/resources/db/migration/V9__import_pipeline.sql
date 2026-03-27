-- ========================================
-- NovaLedger Schema
-- Flyway Migration V9
-- Import Pipeline：UPLOAD_JOBS、UPLOAD_FILES、
--                  PARSED_RECORDS、IMPORT_LOGS
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- UPLOAD_JOBS
-- 設計重點：
--   tenant-aware
--   代表一次完整的上傳／匯入流程
--   不存實際資料內容，只存流程狀態與統計資訊
--   TOTAL_COUNT / SUCCESS_COUNT / FAIL_COUNT 為 Optional
--   （任務建立時尚未知道總筆數）
--   idx_upload_jobs_status → 查詢租戶任務狀態主路徑
-- ========================================

CREATE TABLE upload_jobs (
    -- PK
                             ID            BIGINT      AUTO_INCREMENT PRIMARY KEY,      -- 任務主鍵

    -- 租戶 & 建立者
                             TENANT_ID     BIGINT      NOT NULL,                        -- 所屬帳本
                             CREATED_BY    BIGINT      NOT NULL,                        -- 任務建立者

    -- 任務資訊
                             JOB_TYPE      VARCHAR(50) NOT NULL,                        -- 匯入類型，如 CREDIT_CARD / BANK
                             STATUS        VARCHAR(30) NOT NULL,                        -- PENDING / PROCESSING / DONE / FAILED

    -- 統計（任務完成後才有值）
                             TOTAL_COUNT   INT,                                         -- 總資料筆數（Optional）
                             SUCCESS_COUNT INT,                                         -- 成功筆數（Optional）
                             FAIL_COUNT    INT,                                         -- 失敗筆數（Optional）

    -- 時間
                             CREATED_AT    DATETIME    NOT NULL,                        -- 建立時間
                             FINISHED_AT   DATETIME,                                    -- 完成時間（Optional）

    -- Index
                             INDEX idx_upload_jobs_status        (TENANT_ID, STATUS, CREATED_AT),  -- 查詢租戶任務狀態主路徑
                             INDEX idx_upload_jobs_created_by    (CREATED_BY),                     -- 查詢使用者的任務

    -- FK
                             CONSTRAINT fk_uj_tenant
                                 FOREIGN KEY (TENANT_ID)
                                     REFERENCES tenants(ID)
                                     ON DELETE CASCADE,

                             CONSTRAINT fk_uj_created_by
                                 FOREIGN KEY (CREATED_BY)
                                     REFERENCES users(ID)
                                     ON DELETE RESTRICT            -- 使用者刪除時不可連帶刪除任務記錄

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ========================================
-- UPLOAD_FILES
-- 設計重點：
--   tenant-aware
--   一個匯入任務可包含多個檔案
--   STORED_FILENAME 於同租戶內唯一（避免儲存衝突）
--   不綁定實體儲存位置（S3 / local 由應用層決定）
--   UPLOADED_AT 即為建立時間，不額外補 CREATED_AT
--   uk_upload_files_tenant_filename → 同租戶儲存檔名唯一
--   idx_upload_files_job → 查詢任務下所有檔案
-- ========================================

CREATE TABLE upload_files (
    -- PK
                              ID                BIGINT       AUTO_INCREMENT PRIMARY KEY, -- 檔案主鍵

    -- 租戶 & 任務
                              TENANT_ID         BIGINT       NOT NULL,                   -- 所屬帳本
                              UPLOAD_JOB_ID     BIGINT       NOT NULL,                   -- 所屬任務

    -- 檔案資訊
                              ORIGINAL_FILENAME VARCHAR(255) NOT NULL,                   -- 使用者上傳的原始檔名
                              STORED_FILENAME   VARCHAR(255) NOT NULL,                   -- 系統儲存名稱（UUID 命名）
                              FILE_SIZE         BIGINT       NOT NULL,                   -- 檔案大小（bytes）
                              MIME_TYPE         VARCHAR(100),                            -- 檔案類型（Optional）

    -- 時間
                              UPLOADED_AT       DATETIME     NOT NULL,                   -- 上傳完成時間

    -- Unique：同租戶下儲存檔名唯一
                              UNIQUE KEY uk_upload_files_tenant_filename  (TENANT_ID, STORED_FILENAME),

    -- Index
                              INDEX idx_upload_files_job          (TENANT_ID, UPLOAD_JOB_ID),  -- 查詢任務下所有檔案

    -- FK
                              CONSTRAINT fk_uf_tenant
                                  FOREIGN KEY (TENANT_ID)
                                      REFERENCES tenants(ID)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_uf_job
                                  FOREIGN KEY (UPLOAD_JOB_ID)
                                      REFERENCES upload_jobs(ID)
                                      ON DELETE CASCADE             -- 任務刪除時檔案記錄一併刪除

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ========================================
-- PARSED_RECORDS
-- 設計重點：
--   tenant-aware，高寫入量暫存表
--   儲存上傳檔案解析後的暫存資料，供後續檢核與正式匯入
--   RAW_DATA 為 JSON 型態，資料量大，不加入複合索引
--   PARSE_STATUS 為主要查詢條件，加入複合索引
--   idx_parsed_records_job_status → 查詢任務解析結果主路徑
-- ========================================

CREATE TABLE parsed_records (
    -- PK
                                ID             BIGINT      AUTO_INCREMENT PRIMARY KEY,     -- 主鍵

    -- 租戶 & 任務 & 檔案
                                TENANT_ID      BIGINT      NOT NULL,                       -- 所屬帳本
                                UPLOAD_JOB_ID  BIGINT      NOT NULL,                       -- 所屬任務
                                UPLOAD_FILE_ID BIGINT      NOT NULL,                       -- 來源檔案

    -- 解析資訊
                                SOURCE_ROW_NUM INT         NOT NULL,                       -- 檔案原始列號
                                RAW_DATA       JSON        NOT NULL,                       -- 原始內容（JSON）
                                PARSE_STATUS   VARCHAR(30) NOT NULL,                       -- PENDING / SUCCESS / FAILED
                                ERROR_MESSAGE  TEXT,                                       -- 解析錯誤訊息（Optional）

    -- 稽核欄位
                                CREATED_AT     DATETIME    NOT NULL,                       -- 建立時間

    -- Index
                                INDEX idx_parsed_records_job_status (TENANT_ID, UPLOAD_JOB_ID, PARSE_STATUS), -- 查詢解析結果主路徑
                                INDEX idx_parsed_records_file       (UPLOAD_FILE_ID),                          -- 查詢檔案所有解析記錄

    -- FK
                                CONSTRAINT fk_pr_tenant
                                    FOREIGN KEY (TENANT_ID)
                                        REFERENCES tenants(ID)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_pr_job
                                    FOREIGN KEY (UPLOAD_JOB_ID)
                                        REFERENCES upload_jobs(ID)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_pr_file
                                    FOREIGN KEY (UPLOAD_FILE_ID)
                                        REFERENCES upload_files(ID)
                                        ON DELETE CASCADE             -- 檔案刪除時解析記錄一併刪除

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ========================================
-- IMPORT_LOGS
-- 設計重點：
--   tenant-aware
--   記錄匯入流程中的行為與錯誤，用於除錯與後台查詢
--   僅新增不修改，所以只有 CREATED_AT，沒有 UPDATED_AT
--   MESSAGE 為 TEXT 型態，不可加 INDEX
--   idx_import_logs_job → 查詢任務所有日誌主路徑
--   idx_import_logs_level_time → 按日誌等級 + 時間查詢（後台監控用）
-- ========================================

CREATE TABLE import_logs (
    -- PK
                             ID            BIGINT      AUTO_INCREMENT PRIMARY KEY,      -- 日誌主鍵

    -- 租戶 & 任務
                             TENANT_ID     BIGINT      NOT NULL,                        -- 所屬帳本
                             UPLOAD_JOB_ID BIGINT      NOT NULL,                        -- 所屬任務
                             RECORD_ID     BIGINT,                                      -- 對應解析資料（Optional）

    -- 日誌內容
                             LOG_LEVEL     VARCHAR(20) NOT NULL,                        -- INFO / WARN / ERROR
                             MESSAGE       TEXT        NOT NULL,                        -- 訊息內容

    -- 稽核欄位（僅新增不修改，只有 CREATED_AT）
                             CREATED_AT    DATETIME    NOT NULL,                        -- 建立時間

    -- Index
                             INDEX idx_import_logs_job           (TENANT_ID, UPLOAD_JOB_ID, CREATED_AT),  -- 查詢任務日誌主路徑
                             INDEX idx_import_logs_level_time    (LOG_LEVEL, CREATED_AT),                 -- 後台監控查詢

    -- FK
                             CONSTRAINT fk_il_tenant
                                 FOREIGN KEY (TENANT_ID)
                                     REFERENCES tenants(ID)
                                     ON DELETE CASCADE,

                             CONSTRAINT fk_il_job
                                 FOREIGN KEY (UPLOAD_JOB_ID)
                                     REFERENCES upload_jobs(ID)
                                     ON DELETE CASCADE,

                             CONSTRAINT fk_il_record
                                 FOREIGN KEY (RECORD_ID)
                                     REFERENCES parsed_records(ID)
                                     ON DELETE SET NULL            -- 解析記錄刪除時，日誌保留但 RECORD_ID 設 NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


SET FOREIGN_KEY_CHECKS = 1;