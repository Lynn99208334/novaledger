package com.example.novaledger.finance.importjob.controller;

import com.example.novaledger.common.tenant.AuthContext;
import com.example.novaledger.finance.importjob.dto.UploadJobResponse;
import com.example.novaledger.finance.importjob.service.ImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImportController.class)
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportService importService;

    @MockitoBean
    private AuthContext authContext;

    @Test
    @DisplayName("上傳合法 csv 檔案 → 回 200，jobId 存在")
    void uploadFile_validCsv_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "往來明細.csv",
                "text/csv",
                "date,amount\n2026-01-01,100".getBytes()
        );

        UploadJobResponse mockResponse = new UploadJobResponse();
        mockResponse.setJobId(1L);
        mockResponse.setStatus("PENDING");
        mockResponse.setJobType("ACCOUNT");
        mockResponse.setOriginalFilename("往來明細.csv");
        mockResponse.setCreatedAt(LocalDateTime.now());

        when(authContext.getCurrentTenantId(any())).thenReturn(1L);
        when(authContext.getCurrentUserId(any())).thenReturn(1L);
        when(importService.createUploadJob(any(), anyString(), anyString(), anyLong(), anyLong(), anyLong()))
                .thenReturn(mockResponse);

        mockMvc.perform(multipart("/api/import/upload")
                        .file(file)
                        .param("jobType", "ACCOUNT")
                        .param("bankCode", "822")
                        .param("accountId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }
}