package com.example.novaledger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    public void sendVerifyEmail(String toEmail, String verifyLink) {
        // 🚧 暫時先用 log，後面再換成真的寄信
        log.info("Send verify email to {}", toEmail);
        log.info("\uD83D\uDC49 Verify link: {}", verifyLink);
    }
}
