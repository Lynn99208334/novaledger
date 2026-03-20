package com.example.novaledger.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseTenantEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}