package com.example.novaledger.common.menu;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 依目前登入者角色，篩選並分組 Sidebar 選單項目。
 */
@Service
public class MenuService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * 取得目前登入者可見的選單項目，依 NavigationMenuConfig 中的 section 順序分組。
     *
     * @return key 為 section 名稱（"Core"、"功能區"、"系統管理"、"後台管理"），
     *         value 為該 section 下使用者可見的 MenuItem 清單（保留原始順序）。
     *         未登入或匿名使用者回傳空 Map。
     */
    public Map<String, List<MenuItem>> getMenuItemsForCurrentUser() {
        MenuRole currentRole = resolveCurrentRole();
        if (currentRole == null) {
            return Map.of();
        }

        Map<String, List<MenuItem>> sections = new LinkedHashMap<>();
        for (MenuItem item : NavigationMenuConfig.ALL_MENU_ITEMS) {
            if (!isVisibleTo(item, currentRole)) {
                continue;
            }
            sections.computeIfAbsent(item.section(), key -> new ArrayList<>()).add(item);
        }
        return sections;
    }

    /**
     * 回傳目前登入者的角色；未登入或匿名使用者回傳 null。
     */
    private MenuRole resolveCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(ROLE_ADMIN));
        return isAdmin ? MenuRole.ADMIN : MenuRole.USER;
    }

    private boolean isVisibleTo(MenuItem item, MenuRole currentRole) {
        return item.requiredRoles().isEmpty() || item.requiredRoles().contains(currentRole);
    }
}
