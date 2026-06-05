package com.example.novaledger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailSender emailSender;

    @Async
    public void sendVerifyEmail(String toEmail, String verifyLink) {
        emailSender.sendVerifyEmail(toEmail, verifyLink);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        emailSender.sendPasswordResetEmail(toEmail, resetLink);
    }
}
