package com.example.tableaudemov2.testutil;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;

public class TestSecurityContextFactory {

    public static Authentication authenticatedAdmin() {
        return new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                Collections.emptyList()
        );
    }
}
