package com.example.novaledger.finance.account.repository;

import com.example.novaledger.finance.account.entity.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Long> {

    List<AccountBalance> findByTenantIdAndAccountId(Long tenantId, Long accountId);

    Optional<AccountBalance> findByTenantIdAndAccountIdAndSnapshotDate(Long tenantId, Long accountId, LocalDate snapshotDate);

    // 報表用：查詢某 tenant 在指定日期區間內的所有快照，依日期排序
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.tenantId = :tenantId " +
           "AND ab.snapshotDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ab.snapshotDate ASC")
    List<AccountBalance> findByTenantIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 報表用：查詢某 tenant 在指定日期區間內的最新一筆快照（每月最後一天）
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.tenantId = :tenantId " +
           "AND ab.snapshotDate IN :dates " +
           "ORDER BY ab.snapshotDate ASC")
    List<AccountBalance> findByTenantIdAndSnapshotDateIn(
            @Param("tenantId") Long tenantId,
            @Param("dates") List<LocalDate> dates);

    // 排程用：確認某帳戶某日期是否已有快照（避免重複寫入）
    boolean existsByTenantIdAndAccountIdAndSnapshotDate(Long tenantId, Long accountId, LocalDate snapshotDate);
}
