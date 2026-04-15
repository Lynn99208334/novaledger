package com.example.novaledger.finance.importrecord.repository;

import com.example.novaledger.finance.importrecord.entity.ImportLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportLogRepository extends JpaRepository<ImportLog, Long> {

    List<ImportLog> findByUploadJobIdAndTenantId(Long uploadJobId, Long tenantId);
}