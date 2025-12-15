package com.example.tableaudemov2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/secure-test")
    public String secureTest() {
        return "AUTH OK";
    }
}