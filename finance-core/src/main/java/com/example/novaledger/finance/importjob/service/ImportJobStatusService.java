package com.example.novaledger.finance.importjob.service;

import com.example.novaledger.finance.importjob.repository.UploadJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ImportJobStatusService {

    private final UploadJobRepository uploadJobRepository;

    public ImportJobStatusService(UploadJobRepository uploadJobRepository) {
        this.uploadJobRepository = uploadJobRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markJobFailed(Long jobId, Long tenantId) {
        uploadJobRepository.findByIdAndTenantId(jobId, tenantId).ifPresent(job -> {
            job.setStatus("FAILED");
            job.setFinishedAt(LocalDateTime.now());
            uploadJobRepository.save(job);
        });
    }
}