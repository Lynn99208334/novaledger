-- ===============================
-- RBAC 初始資料庫版本 (V1)
-- 建立 USERS / ROLE / PERMISSION
-- USER_ROLE / ROLE_PERMISSION / EMAIL_VERIFICATION / PASSWORD_RESET
-- ===============================

-- 1. 使用者主表
CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(100) NOT NULL UNIQUE,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       enabled BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. 角色表
CREATE TABLE roles (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       role_name VARCHAR(100) NOT NULL UNIQUE,
                       description VARCHAR(255)
);

-- 3. 權限表
CREATE TABLE permissions (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             permission_key VARCHAR(150) NOT NULL UNIQUE,
                             description VARCHAR(255)
);

-- 4. 使用者與角色關聯表 (多對多)
CREATE TABLE user_role (
                           user_id BIGINT NOT NULL,
                           role_id BIGINT NOT NULL,
                           PRIMARY KEY (user_id, role_id),
                           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                           FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 5. 角色與權限關聯表 (多對多)
CREATE TABLE role_permission (
                                 role_id BIGINT NOT NULL,
                                 permission_id BIGINT NOT NULL,
                                 PRIMARY KEY (role_id, permission_id),
                                 FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
                                 FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- 6. Email 驗證表（註冊時寄出的驗證連結）
CREATE TABLE email_verification (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    user_id BIGINT NOT NULL,
                                    verification_token VARCHAR(255) NOT NULL UNIQUE,
                                    expiry_time DATETIME NOT NULL,
                                    is_used BOOLEAN NOT NULL DEFAULT FALSE,
                                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 7. 忘記密碼表（reset password token）
CREATE TABLE password_reset (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                reset_token VARCHAR(255) NOT NULL UNIQUE,
                                expiry_time DATETIME NOT NULL,
                                is_used BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 8. 預設建立一個最高管理者角色 + 權限 + 測試帳號
INSERT INTO roles (role_name, description) VALUES
                                               ('ADMIN', '最高管理員'),
                                               ('USER', '一般使用者');

INSERT INTO permissions (permission_key, description) VALUES
    ('ALL_PRIVILEGES', '擁有所有系統權限');

INSERT INTO users (username, email, password, enabled)
VALUES (
           'admin',
           'akimoto113915@gmail.com',
           '$2a$10$2nA0KoUJNbvbyuXuZSySOu3W0vVY0COWWwAIh5blxP/.nClMZR0f6',
           TRUE
       );
/* 上方密碼為 BCrypt(gmail的密碼) → 你可改成自己的測試密碼 */

-- 管理員綁定角色
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.role_name = 'ADMIN';

-- 管理員綁定最高權限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.role_name = 'ADMIN' AND p.permission_key = 'ALL_PRIVILEGES';
