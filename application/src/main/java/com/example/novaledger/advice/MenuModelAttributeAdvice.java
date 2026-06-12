package com.example.novaledger.advice;

import com.example.novaledger.common.menu.MenuItem;
import com.example.novaledger.common.menu.MenuService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Map;

/**
 * 將目前登入者可見的 Sidebar 選單項目（依 section 分組）注入所有 Thymeleaf 頁面的 model。
 *
 * 注入的 model attribute "menuSections"：
 *   key   = section 名稱（"Core"、"功能區"、"系統管理"、"後台管理"）
 *   value = 該 section 下使用者可見的 MenuItem 清單
 *
 * navbar.html 的 sidenav fragment 透過 th:each 渲染此 model attribute。
 * 對 @RestController（/api/**）而言，此 model attribute 不會被序列化進回應，
 * 額外的 SecurityContext 查詢成本可忽略。
 */
@ControllerAdvice
public class MenuModelAttributeAdvice {

    private final MenuService menuService;

    public MenuModelAttributeAdvice(MenuService menuService) {
        this.menuService = menuService;
    }

    @ModelAttribute("menuSections")
    public Map<String, List<MenuItem>> menuSections() {
        return menuService.getMenuItemsForCurrentUser();
    }
}
