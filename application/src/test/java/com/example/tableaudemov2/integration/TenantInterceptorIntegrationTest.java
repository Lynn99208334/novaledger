package com.example.tableaudemov2.integration;

import com.example.tableaudemov2.common.tenant.TenantContext;
import com.example.tableaudemov2.common.tenant.TenantInterceptor;
import com.example.tableaudemov2.config.WebMvcConfig;
import com.example.tableaudemov2.controller.TenantTestController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TenantInterceptor 的「架構級整合測試」
 *
 * 驗證：
 * 1. 無 tenant header → 401
 * 2. 有 tenant header → request 正常通過
 * 3. request 結束後 → TenantContext 一定被 clear（afterCompletion）
 */
@WebMvcTest(
        controllers = TenantTestController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class  // ← 關鍵只針對這個測試關閉 Spring Security
)
@Import({
        TenantInterceptor.class,
        WebMvcConfig.class   // ⚠️ 你註冊 interceptor 的 WebMvcConfigurer
})
//@AutoConfigureMockMvc(addFilters = false)
class TenantInterceptorIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    /**
     * 這一段是整個測試的「封印」
     * 只要 afterCompletion 沒清 ThreadLocal，測試一定會炸
     */
    @AfterEach
    void afterEach() {
        assertThat(TenantContext.getTenantId())
                .as("TenantContext should be cleared after request completion")
                .isNull();
    }

    @Test
    void request_without_tenantId_should_return_401() throws Exception {
        mockMvc.perform(get("/test/tenant"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void request_with_tenantId_should_pass_and_clear_context_after_request() throws Exception {
        mockMvc.perform(
                        get("/test/tenant")
                                .header("X-Tenant-Id", "100")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
        // ⚠️ ThreadLocal clear 的驗證在 @AfterEach
    }
}