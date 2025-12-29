package com.example.tableaudemov2.controller;

import com.example.tableaudemov2.dto.RegisterRequest;
import com.example.tableaudemov2.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {

        String jwt = resolveToken(request);
        System.out.println(">>> logout controller jwt=" + jwt);

        if (jwt != null) {
            authService.logout(jwt);
        }

        // 登出就算 token 已過期，也回 200（實務標準）
        return ResponseEntity.ok().build();
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        authService.register(request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {

        authService.verifyEmail(token);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Email 驗證成功，請重新登入"
        ));
    }
}
