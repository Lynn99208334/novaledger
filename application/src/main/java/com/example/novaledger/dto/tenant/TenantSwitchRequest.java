package com.example.novaledger.dto.tenant;

public class TenantSwitchRequest {

    private Long tenantId;

    public TenantSwitchRequest() {
    }

    public TenantSwitchRequest(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}