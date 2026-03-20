package com.example.novaledger.auth.enums;

public enum UserStatus {
    REGISTERED,   // 已註冊，尚未完成 Email 驗證
    ACTIVE,       // Email 驗證完成，可登入
    SUSPENDED     // 停權（保留給未來）
}
