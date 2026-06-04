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
        return "dashboard";
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

    @GetMapping("/exchange-rates")
    public String exchangeRatesPage() {
        return "exchangeRates";
    }

    @GetMapping("/import")
    public String importPage() {
        return "import";
    }

    @GetMapping("/admin/banks")
    public String adminBanksPage() {
        return "admin/bankList";
    }

    @GetMapping("/admin/users")
    public String adminUsersPage() {
        return "admin/userList";
    }

    @GetMapping("/verify-email-success")
    public String verifyEmailSuccess() {
        return "verify-email-success";
    }

    @GetMapping("/verify-email-error")
    public String verifyEmailError() {
        return "verify-email-error";
    }
}
