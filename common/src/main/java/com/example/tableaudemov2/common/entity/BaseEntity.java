package com.example.tableaudemov2.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity {

    /**
     * 租戶識別碼
     * - 必須由 Service 層顯性設定
     * - Entity 本身不讀 TenantContext
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    /**
     * 建立時間
     * - 僅在第一次 insert 時設定
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新時間
     * - 每次 update 都會更新
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 軟刪除時間
     * - null   → 正常資料
     * - not null → 已刪除
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /* =======================
     * JPA Lifecycle Callbacks
     * ======================= */

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

    /* ==========
     * Getter / Setter
     * ========== */

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}