package com.example.novaledger.dto;

public class VerifySummary {

    private Long userId;
    private String email;
    private boolean emailVerified;

    public VerifySummary(Long userId, String email, boolean emailVerified) {
        this.userId = userId;
        this.email = email;
        this.emailVerified = emailVerified;
    }

    public Long getId() { return userId; }
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public boolean isEmailVerified() { return emailVerified; }
}
