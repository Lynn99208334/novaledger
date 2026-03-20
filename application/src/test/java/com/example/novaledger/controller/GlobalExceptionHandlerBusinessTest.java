package com.example.novaledger.controller;

import com.example.novaledger.advice.GlobalExceptionHandler;
import com.example.novaledger.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest
@ContextConfiguration(classes = {
        TestExceptionController.class,
        GlobalExceptionHandler.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonAutoConfiguration.class) // 添加這行
public class GlobalExceptionHandlerBusinessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void businessException_should_return_api_response_fail() throws Exception {
        mockMvc.perform(
                        get("/test/business-exception")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode")
                        .value(ErrorCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.error.status").value(404))
                .andExpect(jsonPath("$.error.path")
                        .value("/test/business-exception"));
    }

    @Test
    void runtimeException_should_return_internal_error() throws Exception {
        mockMvc.perform(
                        get("/test/runtime-exception")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode")
                        .value(ErrorCode.INTERNAL_ERROR.getCode()))
                .andExpect(jsonPath("$.error.status").value(500))
                .andExpect(jsonPath("$.error.path")
                        .value("/test/runtime-exception"));
    }

}

