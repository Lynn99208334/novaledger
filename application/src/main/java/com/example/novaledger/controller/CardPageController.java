package com.example.novaledger.controller;

import com.example.novaledger.common.tenant.AuthContext;
import com.example.novaledger.finance.creditcard.dto.CreateCardRequest;
import com.example.novaledger.finance.creditcard.service.CardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/cards")
public class CardPageController {

    private final CardService cardService;
    private final AuthContext authContext;

    public CardPageController(CardService cardService, AuthContext authContext) {
        this.cardService = cardService;
        this.authContext = authContext;
    }

    @GetMapping
    public String listCards(Model model, HttpServletRequest request) {
        Long userId = authContext.getCurrentUserId(request);
        model.addAttribute("cards", cardService.getCards(userId));
        return "cardList";
    }

    @GetMapping("/new")
    public String newCardForm(Model model) {
        model.addAttribute("cardForm", new CreateCardRequest());
        return "cardCreate";
    }

    @PostMapping
    public String createCard(@Valid @ModelAttribute("cardForm") CreateCardRequest form,
                             BindingResult bindingResult,
                             HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return "cardCreate";
        }
        Long userId = authContext.getCurrentUserId(request);
        cardService.createCard(userId, form);
        return "redirect:/cards";
    }
}