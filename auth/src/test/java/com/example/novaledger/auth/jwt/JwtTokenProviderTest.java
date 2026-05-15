package com.example.novaledger.auth.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "test-secret-key-must-be-long-enough-32chars",
                3600000L,
                604800000L
        );
    }

    @Test
    void generateAccessToken_shouldContainCorrectClaims() {
        String token = jwtTokenProvider.generateAccessToken(1L,  List.of("ROLE_USER"));

        Claims claims = jwtTokenProvider.getClaims(token);
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("roles", List.class)).contains("ROLE_USER");
    }

    @Test
    void validateToken_shouldReturnTrue_whenTokenIsValid() {
        String token = jwtTokenProvider.generateAccessToken(1L,  List.of("ROLE_USER"));
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenIsExpired() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
                "test-secret-key-must-be-long-enough-32chars",
                1L,  // 1ms，馬上過期
                604800000L
        );
        String token = shortLivedProvider.generateAccessToken(1L,  List.of("ROLE_USER"));

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThat(shortLivedProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenIsTampered() {
        String token = jwtTokenProvider.generateAccessToken(1L,  List.of("ROLE_USER"));
        String tampered = token + "tampered";
        assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    void generateBCryptPasswordEncoder(){
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("123456789"));
    }
}

