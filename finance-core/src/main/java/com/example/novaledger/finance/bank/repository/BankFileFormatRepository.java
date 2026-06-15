package com.example.novaledger.finance.bank.repository;

import com.example.novaledger.finance.bank.entity.BankFileFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankFileFormatRepository extends JpaRepository<BankFileFormat, Long> {

    boolean existsByParserKey(String parserKey);

    Optional<BankFileFormat> findByParserKey(String parserKey);

    Optional<BankFileFormat> findTopByBankCodeAndFileTypeAndIsActiveOrderByReleasedDateDesc(
            String bankCode, String fileType, Integer isActive);
}
