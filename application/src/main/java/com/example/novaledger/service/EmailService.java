package com.example.novaledger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerifyEmail(String toEmail, String verifyLink) {
        String subject = "【NovaLedger】請驗證您的電子郵件";
        String content = """
                <p>感謝您註冊 NovaLedger！</p>
                <p>請點擊以下連結完成 Email 驗證（15 分鐘內有效）：</p>
                <p><a href="%s">點我驗證 Email</a></p>
                <p>若您沒有註冊過 NovaLedger，請忽略此信。</p>
                """.formatted(verifyLink);
        sendHtmlEmail(toEmail, subject, content);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String subject = "【NovaLedger】重設您的密碼";
        String content = """
                <p>我們收到您的密碼重設申請。</p>
                <p>請點擊以下連結重設密碼（15 分鐘內有效）：</p>
                <p><a href="%s">點我重設密碼</a></p>
                <p>若您沒有申請重設密碼，請忽略此信。</p>
                """.formatted(resetLink);
        sendHtmlEmail(toEmail, subject, content);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("action=EMAIL_SENT to={} subject={}", toEmail, subject);
        } catch (MessagingException e) {
            log.error("action=EMAIL_SEND_FAILED to={} error={}", toEmail, e.getMessage(), e);
        }
    }
}
