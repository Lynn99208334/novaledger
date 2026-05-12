package com.example.novaledger.dto.tenant;

public class TenantResponse {

    private Long tenantId;
    private String tenantCode;
    private String tenantName;
    private String tenantType;
    private String roleCode;
    private Boolean current;

    public TenantResponse() {
    }

    public TenantResponse(Long tenantId,
                          String tenantCode,
                          String tenantName,
                          String tenantType,
                          String roleCode,
                          Boolean current) {
        this.tenantId = tenantId;
        this.tenantCode = tenantCode;
        this.tenantName = tenantName;
        this.tenantType = tenantType;
        this.roleCode = roleCode;
        this.current = current;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getTenantType() {
        return tenantType;
    }

    public void setTenantType(String tenantType) {
        this.tenantType = tenantType;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public Boolean getCurrent() {
        return current;
    }

    public void setCurrent(Boolean current) {
        this.current = current;
    }
}