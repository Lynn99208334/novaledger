package com.example.novaledger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
public class ResendEmailSender implements EmailSender {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final String FROM_ADDRESS = "NovaLedger <onboarding@resend.dev>";

    private final RestTemplate restTemplate;

    @Value("${resend.api-key}")
    private String apiKey;

    public ResendEmailSender() {
        this.restTemplate = new RestTemplate();
    }

    @Override
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

    @Override
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "from", FROM_ADDRESS,
                    "to", new String[]{toEmail},
                    "subject", subject,
                    "html", htmlContent
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(RESEND_API_URL, request, String.class);
            log.info("action=EMAIL_SENT provider=resend to={} subject={}", toEmail, subject);
        } catch (Exception e) {
            log.error("action=EMAIL_SEND_FAILED provider=resend to={} error={}", toEmail, e.getMessage(), e);
        }
    }
}
