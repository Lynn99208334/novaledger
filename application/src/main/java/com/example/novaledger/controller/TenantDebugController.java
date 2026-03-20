package com.example.novaledger.controller;

import com.example.novaledger.common.tenant.TenantContext;
import com.example.novaledger.service.TenantDebugService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
public class TenantDebugController {

    private final TenantDebugService tenantDebugService;

    public TenantDebugController(TenantDebugService tenantDebugService) {
        this.tenantDebugService = tenantDebugService;
    }

    @GetMapping("/tenant")
    public String whoAmI() {
        Long tenantId = TenantContext.getTenantId();
        return tenantDebugService.whoAmI(tenantId);
    }
}

