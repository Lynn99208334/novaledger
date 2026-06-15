-- ========================================
-- NovaLedger Schema
-- Flyway Migration V9
-- Import Pipeline：UPLOAD_JOBS、UPLOAD_FILES、
--                  PARSED_RECORDS、IMPORT_LOGS
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE upload_jobs (
                             ID            BIGINT      AUTO_INCREMENT PRIMARY KEY,
                             ACCOUNT_ID BIGINT NULL,

                             TENANT_ID     BIGINT      NOT NULL,
                             CREATED_BY    BIGINT      NOT NULL,

                             JOB_TYPE      VARCHAR(50) NOT NULL,
                             STATUS        VARCHAR(30) NOT NULL,
                             PARSER_KEY    VARCHAR(100),

                             TOTAL_COUNT   INT,
                             SUCCESS_COUNT INT,
                             FAIL_COUNT    INT,
                             DUP_COUNT     INT,

                             CREATED_AT    DATETIME    NOT NULL,
                             UPDATED_AT    DATETIME,
                             FINISHED_AT   DATETIME,

                             DETECTED_ACCOUNT_NUMBER VARCHAR(50),
                             DETECTED_FORMAT_ID      BIGINT,

                             INDEX idx_upload_jobs_status     (TENANT_ID, STATUS, CREATED_AT),
                             INDEX idx_upload_jobs_created_by (CREATED_BY),
                             INDEX idx_upload_jobs_format     (DETECTED_FORMAT_ID),

                             CONSTRAINT fk_uj_tenant
                                 FOREIGN KEY (TENANT_ID) REFERENCES tenants(ID) ON DELETE CASCADE,
                             CONSTRAINT fk_uj_created_by
                                 FOREIGN KEY (CREATED_BY) REFERENCES users(ID) ON DELETE RESTRICT,
                             CONSTRAINT fk_uj_format
                                 FOREIGN KEY (DETECTED_FORMAT_ID) REFERENCES bank_file_formats(ID) ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE upload_files (
                              ID                BIGINT       AUTO_INCREMENT PRIMARY KEY,

                              TENANT_ID         BIGINT       NOT NULL,
                              UPLOAD_JOB_ID     BIGINT       NOT NULL,

                              ORIGINAL_FILENAME VARCHAR(255) NOT NULL,
                              STORED_FILENAME   VARCHAR(255) NOT NULL,
                              FILE_SIZE         BIGINT       NOT NULL,
                              MIME_TYPE         VARCHAR(100),

                              CREATED_AT        DATETIME     NOT NULL,
                              UPDATED_AT        DATETIME,

                              UNIQUE KEY uk_upload_files_job (UPLOAD_JOB_ID),
                              INDEX idx_upload_files_job (TENANT_ID, UPLOAD_JOB_ID),

                              CONSTRAINT fk_uf_tenant
                                  FOREIGN KEY (TENANT_ID) REFERENCES tenants(ID) ON DELETE CASCADE,
                              CONSTRAINT fk_uf_job
                                  FOREIGN KEY (UPLOAD_JOB_ID) REFERENCES upload_jobs(ID) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE parsed_records (
                                ID             BIGINT      AUTO_INCREMENT PRIMARY KEY,

                                TENANT_ID      BIGINT      NOT NULL,
                                UPLOAD_JOB_ID  BIGINT      NOT NULL,
                                UPLOAD_FILE_ID BIGINT      NOT NULL,

                                SOURCE_ROW_NUM   INT           NOT NULL,
                                RAW_DATA         JSON          NOT NULL,
                                PARSE_STATUS     VARCHAR(30)   NOT NULL,
                                IMPORT_STATUS    VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
                                ERROR_MESSAGE    TEXT,

                                TRANSACTION_DATE DATE,
                                DESCRIPTION      VARCHAR(500),
                                AMOUNT           DECIMAL(15,2),
                                BALANCE          DECIMAL(15,2),
                                CURRENCY_CODE    VARCHAR(10)   DEFAULT 'TWD',
                                DEDUP_KEY        VARCHAR(64)   NULL COMMENT 'SHA-256(accountId+date+amount+balance)，用於去重',

                                CREATED_AT     DATETIME    NOT NULL,
                                UPDATED_AT     DATETIME,

                                UNIQUE KEY uk_parsed_records_dedup (DEDUP_KEY),
                                INDEX idx_parsed_records_job_status (TENANT_ID, UPLOAD_JOB_ID, PARSE_STATUS),
                                INDEX idx_parsed_records_file       (UPLOAD_FILE_ID),
                                INDEX idx_parsed_records_import     (TENANT_ID, IMPORT_STATUS),

                                CONSTRAINT fk_pr_tenant
                                    FOREIGN KEY (TENANT_ID) REFERENCES tenants(ID) ON DELETE CASCADE,
                                CONSTRAINT fk_pr_job
                                    FOREIGN KEY (UPLOAD_JOB_ID) REFERENCES upload_jobs(ID) ON DELETE CASCADE,
                                CONSTRAINT fk_pr_file
                                    FOREIGN KEY (UPLOAD_FILE_ID) REFERENCES upload_files(ID) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE import_logs (
                             ID            BIGINT      AUTO_INCREMENT PRIMARY KEY,

                             TENANT_ID     BIGINT      NOT NULL,
                             UPLOAD_JOB_ID BIGINT      NOT NULL,
                             RECORD_ID     BIGINT,

                             LOG_LEVEL     VARCHAR(20) NOT NULL,
                             MESSAGE       TEXT        NOT NULL,

                             CREATED_AT    DATETIME    NOT NULL,
                             UPDATED_AT    DATETIME,

                             INDEX idx_import_logs_job        (TENANT_ID, UPLOAD_JOB_ID, CREATED_AT),
                             INDEX idx_import_logs_level_time (LOG_LEVEL, CREATED_AT),

                             CONSTRAINT fk_il_tenant
                                 FOREIGN KEY (TENANT_ID) REFERENCES tenants(ID) ON DELETE CASCADE,
                             CONSTRAINT fk_il_job
                                 FOREIGN KEY (UPLOAD_JOB_ID) REFERENCES upload_jobs(ID) ON DELETE CASCADE,
                             CONSTRAINT fk_il_record
                                 FOREIGN KEY (RECORD_ID) REFERENCES parsed_records(ID) ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


SET FOREIGN_KEY_CHECKS = 1;