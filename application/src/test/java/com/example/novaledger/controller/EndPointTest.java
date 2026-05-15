package com.example.novaledger.controller;

import com.example.novaledger.auth.service.RedisBlacklistService;
import com.example.novaledger.common.tenant.AuthContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class EndPointTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private RedisBlacklistService redisBlacklistService;

    @MockitoBean
    com.example.novaledger.auth.jwt.JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    AuthContext authContext;

    @Test
    void health_should_pass_without_tenant_header() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }
}