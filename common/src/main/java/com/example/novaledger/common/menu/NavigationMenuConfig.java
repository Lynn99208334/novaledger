package com.example.novaledger.common.menu;

import java.util.List;
import java.util.Set;

/**
 * 集中定義 Member / Admin 所有 Sidebar 選單項目。
 *
 * 目前選單清單採用 Java List 靜態定義，現階段不進 DB。
 * 若未來需要動態調整選單（例如依方案開關顯示項目），可考慮改為 DB 驅動，
 * 但目前 Phase 不需要這層彈性（Design Priority: Current Phase Fit > Future Scalability）。
 *
 * MenuItem.requiredRoles 只處理 RBAC 角色（ADMIN/USER）。
 * VIP/付費方案屬於 Subscription Plan，是獨立概念，留給未來 G 系列以 requiredPlan 欄位擴充。
 */
public final class NavigationMenuConfig {

    private NavigationMenuConfig() {
    }

    private static final Set<MenuRole> USER_ONLY = Set.of(MenuRole.USER);
    private static final Set<MenuRole> ADMIN_ONLY = Set.of(MenuRole.ADMIN);

    /**
     * 所有選單項目，依畫面顯示順序排列。
     * MenuService 會依目前登入者角色過濾，並依 section 分組。
     */
    public static final List<MenuItem> ALL_MENU_ITEMS = List.of(

            // ── Core ─────────────────────────────────────────
            new MenuItem("Core", "Dashboard", "/page/dashboard",
                    "fas fa-tachometer-alt", USER_ONLY),
            new MenuItem("Core", "Admin Dashboard", "/page/admin/dashboard",
                    "fas fa-tachometer-alt", ADMIN_ONLY),

            // ── 功能區（記帳相關，僅 USER）──────────────────────
            new MenuItem("功能區", "帳戶管理", "/accounts",
                    "fas fa-university", USER_ONLY),
            new MenuItem("功能區", "信用卡管理", "/cards",
                    "fas fa-credit-card", USER_ONLY),
            new MenuItem("功能區", "匯入對帳單", "/page/import",
                    "fas fa-file-import", USER_ONLY),
            new MenuItem("功能區", "交易記錄", "/page/transactions",
                    "fas fa-list-alt", USER_ONLY),

            // ── 系統管理（記帳相關，僅 USER）────────────────────
            new MenuItem("系統管理", "匯率管理", "/page/exchange-rates",
                    "fas fa-coins", USER_ONLY),
            new MenuItem("系統管理", "月報表", "/page/report/monthly",
                    "fas fa-chart-line", USER_ONLY),
            new MenuItem("系統管理", "年報表", "/page/report/yearly",
                    "fas fa-chart-area", USER_ONLY),

            // ── 後台管理（僅 ADMIN）─────────────────────────────
            new MenuItem("後台管理", "使用者管理", "/page/admin/users",
                    "fas fa-users-cog", ADMIN_ONLY),
            new MenuItem("後台管理", "Audit Log", "/page/admin/audit-logs",
                    "fas fa-clipboard-list", ADMIN_ONLY),
            new MenuItem("後台管理", "解析能力總覽", "/page/admin/parsers",
                    "fas fa-file-alt", ADMIN_ONLY),
            new MenuItem("後台管理", "匯率管理", "/page/admin/exchange-rates",
                    "fas fa-coins", ADMIN_ONLY),
            new MenuItem("後台管理", "系統設定", "/page/admin/system-config",
                    "fas fa-cogs", ADMIN_ONLY)
    );
}
