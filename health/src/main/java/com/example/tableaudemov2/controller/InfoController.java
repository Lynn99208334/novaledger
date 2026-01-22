package com.example.tableaudemov2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InfoController {

    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of(
                "app", "NovaLedger",
                "version", "0.1.0-SNAPSHOT"
        );
    }
}