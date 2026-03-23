package com.example.novaledger.controller;

import com.example.novaledger.dto.AuthResponse;
import com.example.novaledger.dto.LoginRequest;
import com.example.novaledger.dto.RegisterRequest;
import com.example.novaledger.dto.ResendVerificationRequest;
import com.example.novaledger.service.AuthService;
import com.example.novaledger.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

//    @PostMapping("/logout")
//    public ResponseEntity<?> logout(HttpServletRequest request) {
//
//        String jwt = resolveToken(request);
//        System.out.println(">>> logout controller jwt=" + jwt);
//
//        if (jwt != null) {
//            authService.logout(jwt);
//        }
//
//        // 登出就算 token 已過期，也回 200（實務標準）
//        return ResponseEntity.ok().build();
//    }

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

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {

        emailVerificationService.verifyEmail(token);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Email 驗證成功，請重新登入"
        ));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationRequest request
    ) {

        emailVerificationService.resendVerificationEmail(request.getEmail());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Verification email resent"
        ));
    }

}
