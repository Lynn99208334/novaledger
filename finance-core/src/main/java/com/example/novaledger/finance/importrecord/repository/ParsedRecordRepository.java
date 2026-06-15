package com.example.novaledger.finance.importrecord.repository;

import com.example.novaledger.finance.importrecord.entity.ParsedRecord;
import com.example.novaledger.finance.enums.ImportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParsedRecordRepository extends JpaRepository<ParsedRecord, Long> {

    List<ParsedRecord> findByUploadJobIdAndTenantId(Long uploadJobId, Long tenantId);

    List<ParsedRecord> findByUploadJobIdAndTenantIdAndParseStatus(Long uploadJobId, Long tenantId, String parseStatus);

    List<ParsedRecord> findByUploadJobIdAndTenantIdAndImportStatus(Long uploadJobId, Long tenantId, ImportStatus importStatus);

    boolean existsByUploadJobIdAndTenantIdAndImportStatus(Long uploadJobId, Long tenantId, ImportStatus importStatus);

    boolean existsByDedupKey(String dedupKey);
}