package com.example.novaledger.integration;

import com.example.novaledger.auth.jwt.JwtTokenProvider;
import com.example.novaledger.auth.service.RedisBlacklistService;
import com.example.novaledger.common.tenant.AuthContext;
import com.example.novaledger.common.tenant.TenantContext;
import com.example.novaledger.common.tenant.TenantInterceptor;
import com.example.novaledger.config.WebMvcConfig;
import com.example.novaledger.controller.TenantTestController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TenantTestController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        }
)
@Import({
        TenantInterceptor.class,
        WebMvcConfig.class
})
class TenantInterceptorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisBlacklistService redisBlacklistService;

    @MockitoBean
    AuthContext authContext;

    private final String uriTemplate = "/api/test/tenant";

    @AfterEach
    void afterEach() {
        assertThat(TenantContext.getTenantId())
                .as("TenantContext should be cleared after request completion")
                .isNull();
    }

    @Test
    void request_without_tenantId_should_return_401() throws Exception {
        Mockito.when(authContext.getCurrentTenantId()).thenReturn(null);

        mockMvc.perform(get(uriTemplate))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void request_with_tenantId_should_pass_and_clear_context_after_request() throws Exception {
        Mockito.when(authContext.getCurrentTenantId()).thenReturn(100L);

        mockMvc.perform(
                        get(uriTemplate)
                                .sessionAttr("tenantId", 100L)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
    }
}