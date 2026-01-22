package com.example.tableaudemov2.controller;

import com.example.tableaudemov2.common.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantTestController {

    @GetMapping("/test/tenant")
    public String testTenant() {
        // 若 Interceptor 正確，這裡一定拿得到 tenantId
        return String.valueOf(TenantContext.getTenantId());
    }
}
