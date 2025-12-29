package com.example.tableaudemov2.service;

import com.example.tableaudemov2.adapter.cache.RedisCacheAdapter;
import com.example.tableaudemov2.dto.RegisterRequest;
import com.example.tableaudemov2.entity.User;
import com.example.tableaudemov2.enums.ErrorCode;
import com.example.tableaudemov2.enums.UserStatus;
import com.example.tableaudemov2.exception.BusinessException;
import com.example.tableaudemov2.repository.UserRepository;
import com.example.tableaudemov2.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@Service
@Transactional
public class AuthService {

    private final RedisCacheAdapter redisCacheAdapter;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(RedisCacheAdapter redisCacheAdapter,
                       JwtUtil jwtUtil,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.redisCacheAdapter = redisCacheAdapter;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ========================
    // 登出（JWT Blacklist）
    // ========================
    public void logout(String jwt) {
        log.info(">>> AuthService.logout called");

        String jti = jwtUtil.getJti(jwt);
        Date expiration = jwtUtil.getExpiration(jwt);

        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        log.info("Put blacklist jti={}, ttl={}", jti, ttlMillis);

        if (ttlMillis > 0) {
            redisCacheAdapter.putBlacklistJti(jti, ttlMillis);
        }
    }

    // ========================
    // 註冊
    // ========================
    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                    ErrorCode.EMAIL_ALREADY_EXISTS.getMessage(),
                    ErrorCode.EMAIL_ALREADY_EXISTS,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(
                    ErrorCode.USERNAME_ALREADY_EXISTS.getMessage(),
                    ErrorCode.USERNAME_ALREADY_EXISTS,
                    HttpStatus.BAD_REQUEST
            );
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setStatus(UserStatus.REGISTERED);
        user.setEmailVerified(false);
        user.setEnabled(true);

        userRepository.save(user);
    }

    public void verifyEmail(String token) {

        User user = userRepository.findByEmailVerifyToken(token)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.EMAIL_TOKEN_INVALID.getMessage(),
                                ErrorCode.EMAIL_TOKEN_INVALID,
                                HttpStatus.BAD_REQUEST
                        )
                );

        // 已驗證過就不重複處理
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }

        user.setEmailVerified(true);
        user.setEmailVerifyToken(null); //驗證成功後 一定要把 token 清空,防止連結被重複使用
        user.setEmailVerifyExpiredAt(LocalDateTime.now());

        userRepository.save(user);
    }
}
