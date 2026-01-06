package com.example.tableaudemov2.controller;

import com.example.tableaudemov2.service.OcrPdfService;
import com.example.tableaudemov2.dto.ParseResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cc")
public class CardStatementController {

    private final OcrPdfService svc;
    public CardStatementController(OcrPdfService svc) { this.svc = svc; }

    @PostMapping(value="/upload", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ParseResult> upload(@RequestPart("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(svc.parseCreditCardStatement(file.getInputStream()));
    }
}

