package com.example.novaledger.service;

public interface EmailSender {
    void sendVerifyEmail(String toEmail, String verifyLink);
    void sendPasswordResetEmail(String toEmail, String resetLink);
}
