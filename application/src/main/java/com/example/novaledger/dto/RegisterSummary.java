package com.example.novaledger.dto;

public class RegisterSummary {

    private Long userId;
    private String username;
    private String email;

    public RegisterSummary(Long userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    public Long getId() { return userId; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}
