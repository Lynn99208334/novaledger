package com.example.novaledger.finance.bank.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_file_formats")
public class BankFileFormat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;

    @Column(name = "file_type", nullable = false, length = 10)
    private String fileType;

    @Column(name = "released_date", nullable = false)
    private LocalDate releasedDate;

    @Column(name = "parser_key", nullable = false, unique = true, length = 100)
    private String parserKey;

    @Column(name = "is_active", nullable = false)
    private Integer isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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

    public Long getId() { return id; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public LocalDate getReleasedDate() { return releasedDate; }
    public void setReleasedDate(LocalDate releasedDate) { this.releasedDate = releasedDate; }

    public String getParserKey() { return parserKey; }
    public void setParserKey(String parserKey) { this.parserKey = parserKey; }

    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
