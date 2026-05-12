package com.example.novaledger.auth.entity;

import com.example.novaledger.auth.enums.UserTenantStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_tenants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_tenant_active",
                columnNames = {"user_id", "tenant_id", "active_key"}
        )
)
public class UserTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserTenantStatus status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "removed_by_user_id")
    private Long removedByUserId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Tenant tenant;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public UserTenantStatus getStatus() {
        return status;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public Long getRemovedByUserId() {
        return removedByUserId;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public void setStatus(UserTenantStatus status) {
        this.status = status;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public void setRemovedByUserId(Long removedByUserId) {
        this.removedByUserId = removedByUserId;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}