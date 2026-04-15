package com.example.novaledger.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/page")
public class PageImportController {

    @GetMapping("/import")
    public String importPage() {
        return "import";
    }
}
