package com.example.novaledger.finance.importjob.controller;

import com.example.novaledger.common.response.ApiResponse;
import com.example.novaledger.common.tenant.AuthContext;
import com.example.novaledger.finance.importjob.dto.JobStatusResponse;
import com.example.novaledger.finance.importjob.dto.UploadJobResponse;
import com.example.novaledger.finance.importjob.service.ImportService;
import com.example.novaledger.finance.importrecord.dto.ParsedRecordErrorResponse;
import com.example.novaledger.finance.importrecord.dto.ParsedRecordPreviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Import", description = "Excel 匯入")
@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportService importService;
    private final AuthContext authContext;

    public ImportController(ImportService importService, AuthContext authContext) {
        this.importService = importService;
        this.authContext = authContext;
    }

    @Operation(summary = "上傳檔案", description = "支援存摺（ACCOUNT）或信用卡（CREDIT_CARD）格式")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadJobResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobType") String jobType,
            @RequestParam("bankCode") String bankCode,
            HttpServletRequest request) {

        Long tenantId = authContext.getCurrentTenantId(request);
        Long userId = authContext.getCurrentUserId(request);
        UploadJobResponse response = importService.createUploadJob(
                file, jobType, bankCode, tenantId, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/jobs/{jobId}/status")
    public ResponseEntity<ApiResponse<JobStatusResponse>> getJobStatus(
            @PathVariable Long jobId,
            HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("tenantId");
        return ResponseEntity.ok(ApiResponse.ok(importService.getJobStatus(jobId, tenantId)));
    }

    @GetMapping("/jobs/{jobId}/preview")
    public ResponseEntity<ApiResponse<List<ParsedRecordPreviewResponse>>> getJobPreview(
            @PathVariable Long jobId,
            HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("tenantId");
        return ResponseEntity.ok(ApiResponse.ok(importService.getJobPreview(jobId, tenantId)));
    }

    @GetMapping("/jobs/{jobId}/errors")
    public ResponseEntity<ApiResponse<List<ParsedRecordErrorResponse>>> getJobErrors(
            @PathVariable Long jobId,
            HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("tenantId");
        return ResponseEntity.ok(ApiResponse.ok(importService.getJobErrors(jobId, tenantId)));
    }
}