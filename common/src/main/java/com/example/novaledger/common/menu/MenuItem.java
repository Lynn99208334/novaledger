package com.example.novaledger.common.menu;

import java.util.Set;

/**
 * Sidebar 選單項目。
 *
 * @param section       選單分組標題，例如 "Core"、"功能區"、"系統管理"、"後台管理"。
 *                      同一個 section 的項目會被歸在同一個 sb-sidenav-menu-heading 下。
 * @param label         顯示文字
 * @param url           連結路徑（th:href 用）
 * @param icon          FontAwesome icon class，例如 "fas fa-university"
 * @param requiredRoles 允許看到此項目的角色集合。
 *                      空集合代表不限角色，所有已登入使用者都看得到。
 */
public record MenuItem(
        String section,
        String label,
        String url,
        String icon,
        Set<MenuRole> requiredRoles
) {
}
