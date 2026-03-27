package com.example.novaledger.integration.tenant;

import com.example.novaledger.auth.jwt.JwtTokenProvider;
import com.example.novaledger.common.tenant.TenantInterceptor;
import com.example.novaledger.controller.TenantDebugController;
import com.example.novaledger.service.TenantDebugService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = TenantDebugController.class)
@Import(TenantInterceptor.class)
class TenantHeaderMissingTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TenantDebugService tenantDebugService;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @Test
    void request_without_tenant_header_should_be_rejected() throws Exception {
        mockMvc.perform(get("/api/debug/tenant"))
                .andExpect(status().isUnauthorized());
    }
}

