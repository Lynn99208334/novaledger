package com.example.tableaudemov2.controller;

import com.example.tableaudemov2.dto.RegisterRequest;
import com.example.tableaudemov2.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PageController {
    private final AuthService authService;

    public PageController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // ✅ 直接在這裡處理表單送出
    @PostMapping("/register")
    public String register(RegisterRequest request) {

        authService.register(request);

        return "redirect:/login?registered";
    }
}

