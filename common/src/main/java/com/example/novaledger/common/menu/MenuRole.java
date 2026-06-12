package com.example.novaledger.common.menu;

/**
 * Sidebar 選單可見性對應的角色。
 *
 * 對應 Spring Security 的 ROLE_USER / ROLE_ADMIN authority，
 * 只處理 RBAC 角色（不處理 Subscription Plan，VIP/付費方案是獨立概念，
 * 見 NavigationMenuConfig 說明）。
 */
public enum MenuRole {
    USER,
    ADMIN
}
