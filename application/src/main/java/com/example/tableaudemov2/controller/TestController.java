package com.example.tableaudemov2.controller;

import com.example.tableaudemov2.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    private final JwtTokenProvider jwtTokenProvider;

    public TestController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/login")
    public ResponseEntity<?> testLogin(Authentication authentication) {

        // Spring Security 已經幫你驗證完成
        String token = jwtTokenProvider.generateToken(authentication);

        return ResponseEntity.ok(Map.of(
                "token", token
        ));
    }
}