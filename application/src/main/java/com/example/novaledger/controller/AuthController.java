package com.example.novaledger.controller;

import com.example.novaledger.auth.dto.RegisterRequest;
import com.example.novaledger.auth.jwt.JwtTokenProvider;
import com.example.novaledger.auth.service.RedisBlacklistService;
import com.example.novaledger.common.response.ApiErrorResponse;
import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.dto.AuthResponse;
import com.example.novaledger.dto.LoginRequest;
import com.example.novaledger.dto.ForgotPasswordRequest;
import com.example.novaledger.dto.ResetPasswordRequest;
import com.example.novaledger.dto.ResendVerificationRequest;
import com.example.novaledger.service.AuthService;
import com.example.novaledger.service.EmailVerificationService;
import com.example.novaledger.service.PasswordResetService;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "註冊與登入")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisBlacklistService redisBlacklistService;

    @PostMapping("/register")
    @Operation(summary = "註冊新帳號")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpSession session,
                                              HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        AuthResponse response = authService.login(request, session, ip);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "登出（JWT 加入黑名單）")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String token = resolveToken(request);

        if (token == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(new ApiErrorResponse(
                            null, "MISSING_TOKEN", 400, "Authorization header is missing or invalid", null)));
        }

        try {
            String jti = jwtTokenProvider.getJti(token);
            long remainingSeconds = jwtTokenProvider.getRemainingSeconds(token);
            redisBlacklistService.blacklist(jti, remainingSeconds);
            log.info("User logged out, jti={}", jti);
            return ResponseEntity.ok(ApiResponse.ok());
        } catch (JwtException e) {
            log.warn("Logout attempted with invalid token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(new ApiErrorResponse(
                            null, "INVALID_TOKEN", 401, "Token is invalid or expired", null)));
        }
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Email 驗證")
    public ResponseEntity<Void> verifyEmail(@RequestParam("token") String token) {
        try {
            emailVerificationService.verifyEmail(token);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/page/verify-email-success")
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/page/verify-email-error")
                    .build();
        }
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "重新寄送驗證信")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "申請忘記密碼（寄重設信）")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重設密碼")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}