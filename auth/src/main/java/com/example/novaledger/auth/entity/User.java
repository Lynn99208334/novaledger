package com.example.novaledger.auth.entity;

import com.example.novaledger.common.entity.BaseEntity;
import com.example.novaledger.auth.enums.UserStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
public class User extends BaseEntity {

    // ========================
    // 基本帳號資訊
    // ========================

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    /**
     * 系統層級啟用狀態（通常永遠為 true）
     * 只有在法遵 / 資安需求才會關掉
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    // ========================
    // 帳號生命週期（核心）
    // ========================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.REGISTERED;

    /**
     * 是否完成 Email 驗證
     */
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    /**
     * Email 驗證完成時間
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // ========================
    // Email 驗證流程
    // ========================

    /**
     * Email 驗證 Token
     */
    @Column(name = "email_verify_token", length = 100)
    private String emailVerifyToken;

    /**
     * Email 驗證 Token 過期時間
     */
    @Column(name = "email_verify_expired_at")
    private LocalDateTime emailVerifyExpiredAt;


    /**
     * 最後一次寄送 Email 驗證信時間
     */
    @Column(name = "email_verify_last_sent_at")
    private LocalDateTime emailVerifyLastSentAt;

    // ========================
    // 忘記密碼流程
    // ========================

    /**
     * 密碼重設 Token
     */
    @Column(name = "password_reset_token", length = 100)
    private String passwordResetToken;

    /**
     * 密碼重設 Token 過期時間
     */
    @Column(name = "password_reset_expired_at")
    private LocalDateTime passwordResetExpiredAt;

    @Column(name = "is_system_admin", nullable = false)
    private Boolean isSystemAdmin = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // ========================
    // Getter / Setter
    // ========================


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getEmailVerifyToken() {
        return emailVerifyToken;
    }

    public void setEmailVerifyToken(String emailVerifyToken) {
        this.emailVerifyToken = emailVerifyToken;
    }

    public LocalDateTime getEmailVerifyExpiredAt() {
        return emailVerifyExpiredAt;
    }

    public void setEmailVerifyExpiredAt(LocalDateTime emailVerifyExpiredAt) {
        this.emailVerifyExpiredAt = emailVerifyExpiredAt;
    }

    public LocalDateTime getEmailVerifyLastSentAt() {
        return emailVerifyLastSentAt;
    }

    public void setEmailVerifyLastSentAt(LocalDateTime emailVerifyLastSentAt) {
        this.emailVerifyLastSentAt = emailVerifyLastSentAt;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public LocalDateTime getPasswordResetExpiredAt() {
        return passwordResetExpiredAt;
    }

    public void setPasswordResetExpiredAt(LocalDateTime passwordResetExpiredAt) {
        this.passwordResetExpiredAt = passwordResetExpiredAt;
    }

    public Boolean getSystemAdmin() {
        return isSystemAdmin;
    }

    public void setSystemAdmin(Boolean systemAdmin) {
        isSystemAdmin = systemAdmin;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
