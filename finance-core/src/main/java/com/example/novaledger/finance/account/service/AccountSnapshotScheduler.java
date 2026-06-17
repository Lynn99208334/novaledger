package com.example.novaledger.finance.account.service;

import com.example.novaledger.finance.account.entity.AccountBalance;
import com.example.novaledger.finance.account.entity.UserAccount;
import com.example.novaledger.finance.account.repository.AccountBalanceRepository;
import com.example.novaledger.finance.account.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 月底帳戶餘額快照排程
 *
 * 每月最後一天 23:59 執行，將所有 active 帳戶的 CURRENT_BALANCE 寫入 account_balances。
 * 無交易的月份也強制寫入（餘額與 CURRENT_BALANCE 相同），確保報表折線圖不斷點。
 * 若同帳戶同日期快照已存在則跳過（idempotent）。
 */
@Component
public class AccountSnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(AccountSnapshotScheduler.class);

    private final UserAccountRepository userAccountRepository;
    private final AccountBalanceRepository accountBalanceRepository;

    public AccountSnapshotScheduler(UserAccountRepository userAccountRepository,
                                    AccountBalanceRepository accountBalanceRepository) {
        this.userAccountRepository = userAccountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
    }

    /**
     * cron = "0 59 23 L * ?" → 每月最後一天 23:59:00 執行
     * L 代表 Last（當月最後一天），Spring @Scheduled 的 cron 支援此語法
     */
    @Scheduled(cron = "0 59 23 L * ?")
    @Transactional
    public void takeMonthlySnapshot() {
        LocalDate today = LocalDate.now();
        log.info("action=MONTHLY_SNAPSHOT status=START date={}", today);

        List<UserAccount> allAccounts = userAccountRepository.findAll().stream()
                .filter(a -> a.getDeletedAt() == null)
                .toList();

        int written = 0;
        int skipped = 0;

        for (UserAccount account : allAccounts) {
            boolean exists = accountBalanceRepository.existsByTenantIdAndAccountIdAndSnapshotDate(
                    account.getTenantId(), account.getId(), today);

            if (exists) {
                skipped++;
                continue;
            }

            AccountBalance snapshot = new AccountBalance();
            snapshot.setTenantId(account.getTenantId());
            snapshot.setAccountId(account.getId());
            snapshot.setSnapshotDate(today);
            snapshot.setBalance(account.getCurrentBalance());
            snapshot.setCurrencyCode(account.getCurrencyCode());

            accountBalanceRepository.save(snapshot);
            written++;
        }

        log.info("action=MONTHLY_SNAPSHOT status=DONE date={} written={} skipped={}", today, written, skipped);
    }

    /**
     * 手動觸發快照（dev 環境測試用，或補跑歷史快照）
     * 由 ReportController 的 /api/report/snapshot/trigger 呼叫
     */
    @Transactional
    public void takeSnapshotForDate(LocalDate date) {
        log.info("action=MANUAL_SNAPSHOT status=START date={}", date);

        List<UserAccount> allAccounts = userAccountRepository.findAll().stream()
                .filter(a -> a.getDeletedAt() == null)
                .toList();

        int written = 0;
        int skipped = 0;

        for (UserAccount account : allAccounts) {
            boolean exists = accountBalanceRepository.existsByTenantIdAndAccountIdAndSnapshotDate(
                    account.getTenantId(), account.getId(), date);

            if (exists) {
                skipped++;
                continue;
            }

            AccountBalance snapshot = new AccountBalance();
            snapshot.setTenantId(account.getTenantId());
            snapshot.setAccountId(account.getId());
            snapshot.setSnapshotDate(date);
            snapshot.setBalance(account.getCurrentBalance());
            snapshot.setCurrencyCode(account.getCurrencyCode());

            accountBalanceRepository.save(snapshot);
            written++;
        }

        log.info("action=MANUAL_SNAPSHOT status=DONE date={} written={} skipped={}", date, written, skipped);
    }
}
