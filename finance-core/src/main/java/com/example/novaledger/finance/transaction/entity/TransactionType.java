package com.example.novaledger.finance.transaction.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_types")
public class TransactionType {

    @Id
    @Column(name = "CODE", nullable = false, length = 30)
    private String code;

    @Column(name = "NAME", nullable = false, length = 50)
    private String name;

    @Column(name = "AMOUNT_SIGN", nullable = false)
    private Integer amountSign;

    @Column(name = "IS_TRANSFER", nullable = false)
    private Boolean isTransfer;

    @Column(name = "AFFECT_ASSET", nullable = false)
    private Boolean affectAsset;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

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

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAmountSign() { return amountSign; }
    public void setAmountSign(Integer amountSign) { this.amountSign = amountSign; }

    public Boolean getIsTransfer() { return isTransfer; }
    public void setIsTransfer(Boolean isTransfer) { this.isTransfer = isTransfer; }

    public Boolean getAffectAsset() { return affectAsset; }
    public void setAffectAsset(Boolean affectAsset) { this.affectAsset = affectAsset; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}