package com.example.novaledger.controller;

import com.example.novaledger.auth.dto.RegisterRequest;
import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.dto.AuthResponse;
import com.example.novaledger.dto.LoginRequest;
import com.example.novaledger.dto.ResendVerificationRequest;
import com.example.novaledger.service.AuthService;
import com.example.novaledger.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "註冊與登入")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    @Operation(summary = "註冊新帳號")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpSession session) {
        AuthResponse response = authService.login(request, session);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Email 驗證")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam("token") String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "重新寄送驗證信")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}