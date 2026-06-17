package com.example.novaledger.finance.report.service;

import com.example.novaledger.finance.account.entity.AccountBalance;
import com.example.novaledger.finance.account.entity.UserAccount;
import com.example.novaledger.finance.account.enums.AccountType;
import com.example.novaledger.finance.account.repository.AccountBalanceRepository;
import com.example.novaledger.finance.account.repository.UserAccountRepository;
import com.example.novaledger.finance.exchangerate.repository.ExchangeRateRepository;
import com.example.novaledger.finance.report.dto.MonthlyReportResponse;
import com.example.novaledger.finance.report.dto.MonthlySummaryResponse;
import com.example.novaledger.finance.report.dto.YearlyReportResponse;
import com.example.novaledger.finance.report.dto.YearlySummaryResponse;
import com.example.novaledger.finance.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String BASE_CURRENCY = "TWD";
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final AccountBalanceRepository accountBalanceRepository;
    private final UserAccountRepository userAccountRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final TransactionRepository transactionRepository;

    public ReportService(AccountBalanceRepository accountBalanceRepository,
                         UserAccountRepository userAccountRepository,
                         ExchangeRateRepository exchangeRateRepository,
                         TransactionRepository transactionRepository) {
        this.accountBalanceRepository = accountBalanceRepository;
        this.userAccountRepository = userAccountRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.transactionRepository = transactionRepository;
    }

    // ═══════════════════════════════════════════════════════════
    // 月報表 Summary
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(Long tenantId, YearMonth month) {
        YearMonth thisMonth = YearMonth.now();
        boolean isCurrentMonth = month.equals(thisMonth);
        Map<String, BigDecimal> rateCache = new HashMap<>();

        // 當月快照（非當月用此作為「今月餘額」；當月無快照，用 current balance）
        LocalDate monthEnd = month.atEndOfMonth();
        LocalDate monthStart = month.atDay(1);
        List<AccountBalance> thisMonthSnapshots = accountBalanceRepository
                .findByTenantIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                        tenantId, monthEnd, monthEnd);
        Map<Long, BigDecimal> thisMonthMap = new HashMap<>();
        for (AccountBalance ab : thisMonthSnapshots) {
            thisMonthMap.put(ab.getAccountId(), ab.getBalance());
        }

        // 上月底快照（整月查，取最後一筆）
        YearMonth lastMonth = month.minusMonths(1);
        List<AccountBalance> lastMonthSnapshots = accountBalanceRepository
                .findByTenantIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                        tenantId, lastMonth.atDay(1), lastMonth.atEndOfMonth());
        Map<Long, BigDecimal> lastMonthMap = new HashMap<>();
        for (AccountBalance ab : lastMonthSnapshots) {
            lastMonthMap.put(ab.getAccountId(), ab.getBalance());
        }

        // 當月收支
        BigDecimal totalIncome = nullSafe(transactionRepository.sumByTenantIdAndTxTypeCodeAndDateRange(
                tenantId, "INCOME", monthStart, monthEnd));
        BigDecimal totalExpense = nullSafe(transactionRepository.sumByTenantIdAndTxTypeCodeAndDateRange(
                tenantId, "EXPENSE", monthStart, monthEnd));

        // 取所有帳戶，過濾：當月才顯示（選項A：有快照或是當月）
        List<UserAccount> allAccounts = userAccountRepository.findByTenantIdAndDeletedAtIsNull(tenantId);

        List<MonthlySummaryResponse.AccountDetail> accountDetails = new ArrayList<>();
        BigDecimal totalAssetTwd = BigDecimal.ZERO;
        BigDecimal lastTotalTwd = BigDecimal.ZERO;

        for (UserAccount account : allAccounts) {
            boolean hasCurrentSnapshot = thisMonthMap.containsKey(account.getId());
            boolean hasLastSnapshot = lastMonthMap.containsKey(account.getId());

            // 選項A：非當月且當月和上月都沒有快照，不顯示此帳戶
            if (!isCurrentMonth && !hasCurrentSnapshot && !hasLastSnapshot) continue;

            BigDecimal rate = getRate(rateCache, account.getCurrencyCode());

            // 當月餘額：當月用 current balance；非當月用當月快照（無快照為 0）
            BigDecimal current = isCurrentMonth
                    ? account.getCurrentBalance()
                    : thisMonthMap.getOrDefault(account.getId(), BigDecimal.ZERO);
            BigDecimal currentTwd = current.multiply(rate).setScale(0, RoundingMode.HALF_UP);

            BigDecimal last = lastMonthMap.getOrDefault(account.getId(), BigDecimal.ZERO);
            BigDecimal lastTwd = last.multiply(rate).setScale(0, RoundingMode.HALF_UP);

            MonthlySummaryResponse.AccountDetail d = new MonthlySummaryResponse.AccountDetail();
            d.setAccountId(account.getId());
            d.setName(displayName(account));
            d.setAccountType(account.getAccountType().name());
            d.setAccountTypeDisplay(account.getAccountType().getDisplayName());
            d.setCurrencyCode(account.getCurrencyCode());
            d.setBalanceOriginal(current);
            d.setBalanceTwd(currentTwd);
            d.setLastMonthBalanceOriginal(last);
            d.setLastMonthBalanceTwd(lastTwd);
            d.setChangeOriginal(current.subtract(last));
            d.setChangeTwd(currentTwd.subtract(lastTwd));
            d.setChangePercent(pct(currentTwd.subtract(lastTwd), lastTwd));
            accountDetails.add(d);

            totalAssetTwd = totalAssetTwd.add(currentTwd);
            lastTotalTwd = lastTotalTwd.add(lastTwd);
        }

        MonthlySummaryResponse.Summary summary = new MonthlySummaryResponse.Summary();
        summary.setTotalAssetTwd(totalAssetTwd);
        summary.setLastMonthTotalAssetTwd(lastTotalTwd);
        summary.setNetChangeTwd(totalAssetTwd.subtract(lastTotalTwd));
        summary.setNetChangePercent(pct(totalAssetTwd.subtract(lastTotalTwd), lastTotalTwd));
        summary.setTotalIncomeTwd(totalIncome);
        summary.setTotalExpenseTwd(totalExpense);
        summary.setNetIncomeTwd(totalIncome.subtract(totalExpense));

        MonthlySummaryResponse response = new MonthlySummaryResponse();
        response.setReportMonth(month.format(MONTH_FORMATTER));
        response.setSummary(summary);
        response.setTypeGroups(buildMonthlyTypeGroups(accountDetails, totalAssetTwd));
        response.setAccountDetails(accountDetails);
        return response;
    }

    private List<MonthlySummaryResponse.AccountTypeGroup> buildMonthlyTypeGroups(
            List<MonthlySummaryResponse.AccountDetail> details, BigDecimal totalTwd) {
        Map<AccountType, BigDecimal> typeTotal = new LinkedHashMap<>();
        for (AccountType t : AccountType.values()) typeTotal.put(t, BigDecimal.ZERO);
        for (MonthlySummaryResponse.AccountDetail d : details) {
            typeTotal.merge(AccountType.valueOf(d.getAccountType()), d.getBalanceTwd(), BigDecimal::add);
        }
        List<MonthlySummaryResponse.AccountTypeGroup> groups = new ArrayList<>();
        for (Map.Entry<AccountType, BigDecimal> e : typeTotal.entrySet()) {
            MonthlySummaryResponse.AccountTypeGroup g = new MonthlySummaryResponse.AccountTypeGroup();
            g.setType(e.getKey().name());
            g.setDisplayName(e.getKey().getDisplayName());
            g.setTotalTwd(e.getValue());
            g.setPercent(totalTwd.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : e.getValue().multiply(ONE_HUNDRED).divide(totalTwd, 1, RoundingMode.HALF_UP));
            groups.add(g);
        }
        return groups;
    }

    // ═══════════════════════════════════════════════════════════
    // 年報表 Summary
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public YearlySummaryResponse getYearlySummary(Long tenantId, int year) {
        List<UserAccount> allAccounts = userAccountRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
        Map<String, BigDecimal> rateCache = new HashMap<>();
        int thisYear = LocalDate.now().getYear();

        // 今年快照：歷史年份查該年最後一筆
        Map<Long, BigDecimal> thisYearMap = new HashMap<>();
        if (year < thisYear) {
            List<AccountBalance> snaps = accountBalanceRepository
                    .findByTenantIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                            tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
            for (AccountBalance ab : snaps) thisYearMap.put(ab.getAccountId(), ab.getBalance());
        }

        // 去年最後一筆快照
        Map<Long, BigDecimal> lastYearMap = new HashMap<>();
        List<AccountBalance> lastYearSnaps = accountBalanceRepository
                .findByTenantIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                        tenantId, LocalDate.of(year - 1, 1, 1), LocalDate.of(year - 1, 12, 31));
        for (AccountBalance ab : lastYearSnaps) lastYearMap.put(ab.getAccountId(), ab.getBalance());

        // 收支範圍
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = (year == thisYear) ? LocalDate.now() : LocalDate.of(year, 12, 31);
        BigDecimal totalIncome = nullSafe(transactionRepository.sumByTenantIdAndTxTypeCodeAndDateRange(
                tenantId, "INCOME", yearStart, yearEnd));
        BigDecimal totalExpense = nullSafe(transactionRepository.sumByTenantIdAndTxTypeCodeAndDateRange(
                tenantId, "EXPENSE", yearStart, yearEnd));

        List<YearlySummaryResponse.AccountDetail> accountDetails = new ArrayList<>();
        BigDecimal totalAssetTwd = BigDecimal.ZERO;
        BigDecimal lastYearTotalTwd = BigDecimal.ZERO;

        for (UserAccount account : allAccounts) {
            boolean hasThisYear = (year == thisYear) || thisYearMap.containsKey(account.getId());
            boolean hasLastYear = lastYearMap.containsKey(account.getId());

            // 選項A：今年和去年都沒有快照（且非當年），不顯示
            if (year != thisYear && !hasThisYear && !hasLastYear) continue;

            BigDecimal rate = getRate(rateCache, account.getCurrencyCode());
            BigDecimal current = (year == thisYear)
                    ? account.getCurrentBalance()
                    : thisYearMap.getOrDefault(account.getId(), BigDecimal.ZERO);
            BigDecimal currentTwd = current.multiply(rate).setScale(0, RoundingMode.HALF_UP);
            BigDecimal last = lastYearMap.getOrDefault(account.getId(), BigDecimal.ZERO);
            BigDecimal lastTwd = last.multiply(rate).setScale(0, RoundingMode.HALF_UP);

            YearlySummaryResponse.AccountDetail d = new YearlySummaryResponse.AccountDetail();
            d.setAccountId(account.getId());
            d.setName(displayName(account));
            d.setAccountType(account.getAccountType().name());
            d.setAccountTypeDisplay(account.getAccountType().getDisplayName());
            d.setCurrencyCode(account.getCurrencyCode());
            d.setBalanceOriginal(current);
            d.setBalanceTwd(currentTwd);
            d.setLastYearBalanceOriginal(last);
            d.setLastYearBalanceTwd(lastTwd);
            d.setChangeOriginal(current.subtract(last));
            d.setChangeTwd(currentTwd.subtract(lastTwd));
            d.setChangePercent(pct(currentTwd.subtract(lastTwd), lastTwd));
            accountDetails.add(d);

            totalAssetTwd = totalAssetTwd.add(currentTwd);
            lastYearTotalTwd = lastYearTotalTwd.add(lastTwd);
        }

        YearlySummaryResponse.Summary summary = new YearlySummaryResponse.Summary();
        summary.setTotalAssetTwd(totalAssetTwd);
        summary.setLastYearTotalAssetTwd(lastYearTotalTwd);
        summary.setNetChangeTwd(totalAssetTwd.subtract(lastYearTotalTwd));
        summary.setNetChangePercent(pct(totalAssetTwd.subtract(lastYearTotalTwd), lastYearTotalTwd));
        summary.setTotalIncomeTwd(totalIncome);
        summary.setTotalExpenseTwd(totalExpense);
        summary.setNetIncomeTwd(totalIncome.subtract(totalExpense));

        YearlySummaryResponse response = new YearlySummaryResponse();
        response.setReportYear(String.valueOf(year));
        response.setSummary(summary);
        response.setTypeGroups(buildYearlyTypeGroups(accountDetails, totalAssetTwd));
        response.setAccountDetails(accountDetails);
        return response;
    }

    private List<YearlySummaryResponse.AccountTypeGroup> buildYearlyTypeGroups(
            List<YearlySummaryResponse.AccountDetail> details, BigDecimal totalTwd) {
        Map<AccountType, BigDecimal> typeTotal = new LinkedHashMap<>();
        for (AccountType t : AccountType.values()) typeTotal.put(t, BigDecimal.ZERO);
        for (YearlySummaryResponse.AccountDetail d : details) {
            typeTotal.merge(AccountType.valueOf(d.getAccountType()), d.getBalanceTwd(), BigDecimal::add);
        }
        List<YearlySummaryResponse.AccountTypeGroup> groups = new ArrayList<>();
        for (Map.Entry<AccountType, BigDecimal> e : typeTotal.entrySet()) {
            YearlySummaryResponse.AccountTypeGroup g = new YearlySummaryResponse.AccountTypeGroup();
            g.setType(e.getKey().name());
            g.setDisplayName(e.getKey().getDisplayName());
            g.setTotalTwd(e.getValue());
            g.setPercent(totalTwd.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : e.getValue().multiply(ONE_HUNDRED).divide(totalTwd, 1, RoundingMode.HALF_UP));
            groups.add(g);
        }
        return groups;
    }

    // ═══════════════════════════════════════════════════════════
    // 月折線圖（Dashboard 用）
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(Long tenantId, YearMonth start, YearMonth end) {
        List<YearMonth> months = generateMonthRange(start, end);
        List<String> labels = months.stream().map(ym -> ym.format(MONTH_FORMATTER)).toList();

        List<AccountBalance> snapshots = accountBalanceRepository
                .findByTenantIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                        tenantId, start.atDay(1), end.atEndOfMonth());

        List<UserAccount> accounts = userAccountRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
        Map<Long, String> accountNames = accounts.stream()
                .collect(Collectors.toMap(UserAccount::getId, this::buildAccountLabel));

        Map<Long, Map<YearMonth, BigDecimal>> snapshotMap = new HashMap<>();
        for (AccountBalance ab : snapshots) {
            snapshotMap.computeIfAbsent(ab.getAccountId(), k -> new LinkedHashMap<>())
                    .put(YearMonth.from(ab.getSnapshotDate()), ab.getBalance());
        }

        Map<String, List<BigDecimal>> accountSeries = new LinkedHashMap<>();
        for (UserAccount account : accounts) {
            Map<YearMonth, BigDecimal> m = snapshotMap.getOrDefault(account.getId(), Collections.emptyMap());
            List<BigDecimal> series = new ArrayList<>();
            BigDecimal last = BigDecimal.ZERO;
            for (YearMonth ym : months) {
                BigDecimal b = m.get(ym);
                if (b != null) last = b;
                series.add(last);
            }
            accountSeries.put(accountNames.get(account.getId()), series);
        }

        List<BigDecimal> totalAsset = new ArrayList<>();
        for (int i = 0; i < months.size(); i++) {
            final int idx = i;
            totalAsset.add(accountSeries.values().stream()
                    .map(s -> s.get(idx)).reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        MonthlyReportResponse response = new MonthlyReportResponse();
        response.setLabels(labels);
        response.setAccounts(accountSeries);
        response.setTotalAsset(totalAsset);
        return response;
    }

    // ═══════════════════════════════════════════════════════════
    // 年折線圖
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public YearlyReportResponse getYearlyReport(Long tenantId, int startYear, int endYear) {
        List<String> labels = new ArrayList<>();
        List<BigDecimal> totalAsset = new ArrayList<>();

        for (int year = startYear; year <= endYear; year++) {
            labels.add(String.valueOf(year));
            List<AccountBalance> yearSnapshots = accountBalanceRepository
                    .findByTenantIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                            tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
            Map<Long, BigDecimal> latest = new HashMap<>();
            for (AccountBalance ab : yearSnapshots) latest.put(ab.getAccountId(), ab.getBalance());
            totalAsset.add(latest.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        YearlyReportResponse response = new YearlyReportResponse();
        response.setLabels(labels);
        response.setTotalAsset(totalAsset);
        return response;
    }

    // ═══════════════════════════════════════════════════════════
    // 私有工具
    // ═══════════════════════════════════════════════════════════

    private BigDecimal getRate(Map<String, BigDecimal> cache, String currency) {
        if (BASE_CURRENCY.equals(currency)) return BigDecimal.ONE;
        return cache.computeIfAbsent(currency, c ->
                exchangeRateRepository
                        .findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(c, BASE_CURRENCY)
                        .map(er -> er.getRate())
                        .orElse(BigDecimal.ONE));
    }

    private BigDecimal pct(BigDecimal change, BigDecimal base) {
        if (base.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return change.multiply(ONE_HUNDRED).divide(base, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal nullSafe(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private String displayName(UserAccount account) {
        return account.getAlias() != null && !account.getAlias().isBlank()
                ? account.getAlias() : account.getName();
    }

    private List<YearMonth> generateMonthRange(YearMonth start, YearMonth end) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth cur = start;
        while (!cur.isAfter(end)) { months.add(cur); cur = cur.plusMonths(1); }
        return months;
    }

    private String buildAccountLabel(UserAccount account) {
        return displayName(account) + "（" + account.getCurrencyCode() + "）";
    }
}
