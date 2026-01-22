package com.example.tableaudemov2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        System.out.println(">>> HealthController HIT");
        return Map.of("status", "UP");
    }
}
