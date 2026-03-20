package com.example.novaledger.service;

import com.example.novaledger.adapter.cache.RedisCacheAdapter;
import com.example.novaledger.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RedisCacheAdapter redisCacheAdapter;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void logout_should_blacklist_jti() {
        // given
        String jwt = "fake.jwt.token";
        String jti = "test-jti";

        Date expiration = new Date(System.currentTimeMillis() + 60_000); // 1 分鐘後過期

        when(jwtUtil.getJti(jwt)).thenReturn(jti);
        when(jwtUtil.getExpiration(jwt)).thenReturn(expiration);

        // when
        authService.logout(jwt);

        // then
        verify(redisCacheAdapter)
                .putBlacklistJti(eq(jti), anyLong());

    }

}