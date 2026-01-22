package com.example.tableaudemov2.controller;

import com.example.tableaudemov2.common.tenant.TenantInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TenantInterceptor.class)
public class EndPointTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void health_should_pass_without_tenant_header() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }
}
