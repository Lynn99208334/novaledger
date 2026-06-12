package com.example.novaledger.advice;

import com.example.novaledger.common.menu.MenuItem;
import com.example.novaledger.common.menu.MenuService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
 *
 * MenuService 採 Optional<MenuService> 注入：
 * 此類別是全域 @ControllerAdvice，會被每一個 @WebMvcTest 載入；
 * 但 @WebMvcTest 的 context 不包含 @Service bean，若用一般必填的 constructor injection
 * 會導致所有 @WebMvcTest 在 context 載入階段就失敗（UnsatisfiedDependencyException）。
 * Optional<T> 是 Spring 對「可選依賴」的標準解法：找不到 bean 時回傳 Optional.empty()，
 * 不會拋例外；@Autowired(required = false) 在單一建構子情境下對此無效，故不採用。
 * MenuService 本身無任何依賴、無副作用，缺席時直接回空 Map，
 * 不影響這些切片測試原本要驗證的行為。
 */
@ControllerAdvice
public class MenuModelAttributeAdvice {

    private final MenuService menuService;

    public MenuModelAttributeAdvice(Optional<MenuService> menuService) {
        this.menuService = menuService.orElse(null);
    }

    @ModelAttribute("menuSections")
    public Map<String, List<MenuItem>> menuSections() {
        if (menuService == null) {
            return Map.of();
        }
        return menuService.getMenuItemsForCurrentUser();
    }
}
