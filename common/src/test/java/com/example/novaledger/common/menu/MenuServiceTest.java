package com.example.novaledger.common.menu;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MenuServiceTest {

    private final MenuService menuService = new MenuService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMenuItemsForCurrentUser_userRole_shouldOnlyReturnUserSections() {
        authenticateAs("ROLE_USER");

        Map<String, List<MenuItem>> sections = menuService.getMenuItemsForCurrentUser();

        // USER 只會看到記帳相關 section，不會看到「後台管理」
        assertThat(sections).containsKeys("Core", "功能區", "系統管理");
        assertThat(sections).doesNotContainKey("後台管理");

        // Core section 底下只有 Dashboard（USER 用），不會有 Admin Dashboard
        List<String> coreLabels = sections.get("Core").stream()
                .map(MenuItem::label)
                .toList();
        assertThat(coreLabels).containsExactly("Dashboard");
    }

    @Test
    void getMenuItemsForCurrentUser_adminRole_shouldOnlyReturnAdminSections() {
        authenticateAs("ROLE_ADMIN");

        Map<String, List<MenuItem>> sections = menuService.getMenuItemsForCurrentUser();

        // ADMIN 只會看到後台管理相關 section，不會看到記帳功能
        assertThat(sections).containsKeys("Core", "後台管理");
        assertThat(sections).doesNotContainKeys("功能區", "系統管理");

        // Core section 底下只有 Admin Dashboard（ADMIN 用），不會有一般 Dashboard
        List<String> coreLabels = sections.get("Core").stream()
                .map(MenuItem::label)
                .toList();
        assertThat(coreLabels).containsExactly("Admin Dashboard");

        // 後台管理 section 應包含 S14 完成後的五個選單
        List<String> adminLabels = sections.get("後台管理").stream()
                .map(MenuItem::label)
                .toList();
        assertThat(adminLabels).containsExactly("使用者管理", "Audit Log", "解析能力總覽", "匯率管理", "系統設定");
    }

    @Test
    void getMenuItemsForCurrentUser_anonymousUser_shouldReturnEmptyMap() {
        GrantedAuthority anonymousAuthority = new SimpleGrantedAuthority("ROLE_ANONYMOUS");
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", List.of(anonymousAuthority))
        );

        Map<String, List<MenuItem>> sections = menuService.getMenuItemsForCurrentUser();

        assertThat(sections).isEmpty();
    }

    @Test
    void getMenuItemsForCurrentUser_noAuthentication_shouldReturnEmptyMap() {
        SecurityContextHolder.clearContext();

        Map<String, List<MenuItem>> sections = menuService.getMenuItemsForCurrentUser();

        assertThat(sections).isEmpty();
    }

    private void authenticateAs(String roleAuthority) {
        GrantedAuthority authority = new SimpleGrantedAuthority(roleAuthority);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testUser", "password", List.of(authority))
        );
    }
}
