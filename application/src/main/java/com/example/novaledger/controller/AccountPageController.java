package com.example.novaledger.controller;

import com.example.novaledger.common.tenant.AuthContext;
import com.example.novaledger.finance.account.dto.CreateAccountRequest;
import com.example.novaledger.finance.account.enums.AccountType;
import com.example.novaledger.finance.account.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/accounts")
public class AccountPageController {

    private final AccountService accountService;
    private final AuthContext authContext;

    public AccountPageController(AccountService accountService, AuthContext authContext) {
        this.accountService = accountService;
        this.authContext = authContext;
    }

    @GetMapping
    public String listAccounts(Model model, HttpServletRequest request) {
        Long userId = authContext.getCurrentUserId();
        model.addAttribute("accounts", accountService.getAccounts(userId));
        return "accountList";
    }

    @GetMapping("/new")
    public String newAccountForm(Model model) {
        model.addAttribute("accountForm", new CreateAccountRequest());
        model.addAttribute("accountTypes", AccountType.values());
        return "accountCreate";
    }

    @PostMapping
    public String createAccount(@Valid @ModelAttribute("accountForm") CreateAccountRequest form,
                                BindingResult bindingResult,
                                Model model,
                                HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("accountTypes", AccountType.values());
            return "accountCreate";
        }
        Long userId = authContext.getCurrentUserId();
        accountService.createAccount(userId, form);
        return "redirect:/accounts";
    }
}