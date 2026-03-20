package com.example.novaledger.testutil;

import com.example.novaledger.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class TestJwtFactory {

    private static final String SECRET_KEY = "your-secret-key";

    public static String createValidJwt() {
        String jti = JwtUtil.generateJti();

        return Jwts.builder()
                .setId(jti)
                .setSubject("test-user")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                .compact();
    }
}

