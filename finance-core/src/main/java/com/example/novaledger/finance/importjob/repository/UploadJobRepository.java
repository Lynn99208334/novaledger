package com.example.novaledger.finance.importjob.repository;

import com.example.novaledger.finance.importjob.entity.UploadJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UploadJobRepository extends JpaRepository<UploadJob, Long> {

    Optional<UploadJob> findByIdAndTenantId(Long id, Long tenantId);

    List<UploadJob> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    @Query("SELECT COUNT(j) FROM UploadJob j WHERE j.status = 'PENDING'")
    long countPendingJobs();
}
