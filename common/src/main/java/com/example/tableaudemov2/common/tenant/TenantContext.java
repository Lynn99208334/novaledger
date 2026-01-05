package com.example.tableaudemov2.common.tenant;

public final class TenantContext {

    private TenantContext() {
        // 防止被 new
    }

    /**
     * 每一個 request（Thread）都有自己的一份 tenantId
     */
    private static final ThreadLocal<Long> TENANT_ID_HOLDER = new ThreadLocal<>();

    /**
     * 設定 tenantId（request 進來時）
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    /**
     * 取得 tenantId（任何地方都能用）
     */
    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    /**
     * 清除 tenantId（request 結束時，一定要呼叫）
     */
    public static void clear() {
        TENANT_ID_HOLDER.remove();
    }
}