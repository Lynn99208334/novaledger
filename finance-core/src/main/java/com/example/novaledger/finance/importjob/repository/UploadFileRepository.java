package com.example.novaledger.finance.importjob.repository;

import com.example.novaledger.finance.importjob.entity.UploadFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UploadFileRepository extends JpaRepository<UploadFile, Long> {

    Optional<UploadFile> findByUploadJobId(Long uploadJobId);
}