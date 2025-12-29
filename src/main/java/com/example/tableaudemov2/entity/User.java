package com.example.tableaudemov2.entity;

import com.example.tableaudemov2.enums.UserStatus;
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
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    // ========================
    // 系統時間欄位
    // ========================

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========================
    // JPA Lifecycle
    // ========================

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ========================
    // Getter / Setter
    // ========================

    public Long getId() {
        return id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
