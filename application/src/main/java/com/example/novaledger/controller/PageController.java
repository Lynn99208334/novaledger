package com.example.novaledger.controller;

import com.example.novaledger.auth.dto.RegisterRequest;
import com.example.novaledger.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/page")
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

    @GetMapping("/dashboard")
    public String dashboard() {
        return "index";
    }

    @PostMapping("/register")
    public String register(RegisterRequest request) {
        authService.register(request);
        return "redirect:/page/login?registered";
    }

    @GetMapping("/transactions")
    public String transactionsPage() {
        return "transactions";
    }

    @GetMapping("/transactions/new")
    public String transactionNewPage() {
        return "transaction-new";
    }
}