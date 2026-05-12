package com.example.novaledger.finance.dashboard.service;

import com.example.novaledger.finance.account.entity.UserAccount;
import com.example.novaledger.finance.account.repository.UserAccountRepository;
import com.example.novaledger.finance.dashboard.dto.CategoryBreakdownItem;
import com.example.novaledger.finance.dashboard.dto.DashboardSummaryResponse;
import com.example.novaledger.finance.exchangerate.repository.ExchangeRateRepository;
import com.example.novaledger.finance.transaction.entity.Transaction;
import com.example.novaledger.finance.transaction.entity.TransactionItem;
import com.example.novaledger.finance.transaction.repository.TransactionItemRepository;
import com.example.novaledger.finance.transaction.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final String TENANT_TYPE_FAMILY = "FAMILY";
    private static final String TENANT_TYPE_PERSONAL = "PERSONAL";
    private static final String TENANT_STATUS_ACTIVE = "ACTIVE";
    private static final String USER_TENANT_STATUS_ACTIVE = "ACTIVE";

    private final UserAccountRepository userAccountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public DashboardService(
            UserAccountRepository userAccountRepository,
            TransactionRepository transactionRepository,
            TransactionItemRepository transactionItemRepository,
            ExchangeRateRepository exchangeRateRepository) {
        this.userAccountRepository = userAccountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionItemRepository = transactionItemRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    public DashboardSummaryResponse getDashboardSummary(Long tenantId) {
        DashboardSummaryResponse response = new DashboardSummaryResponse();

        List<Long> dashboardTenantIds = resolveDashboardTenantIds(tenantId);

        // 1. 總資產（依幣別分組）
        List<UserAccount> accounts = loadAccounts(dashboardTenantIds);
        response.setHasAccounts(!accounts.isEmpty());

        Map<String, BigDecimal> totalAssetsByCurrency = accounts.stream()
                .collect(Collectors.groupingBy(
                        UserAccount::getCurrencyCode,
                        LinkedHashMap::new,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                account -> account.getCurrentBalance() != null
                                        ? account.getCurrentBalance()
                                        : BigDecimal.ZERO,
                                BigDecimal::add
                        )
                ));

        response.setTotalAssetsByCurrency(totalAssetsByCurrency);

        // 2. 總資產換算成 TWD
        BigDecimal totalInTwd = calculateTotalInTwd(totalAssetsByCurrency);
        response.setTotalAssetsInTwd(totalInTwd);

        // 3. 幣別佔比（換算後 TWD 等值）
        Map<String, BigDecimal> assetsByTwd = calculateAssetsByTwd(totalAssetsByCurrency);
        response.setAssetsByTwd(assetsByTwd);

        // 4. 本月收支
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();

        BigDecimal income = sumTransactionsByTenantIds(
                dashboardTenantIds,
                "INCOME",
                firstDayOfMonth,
                today
        );

        BigDecimal expense = sumTransactionsByTenantIds(
                dashboardTenantIds,
                "EXPENSE",
                firstDayOfMonth,
                today
        );

        response.setMonthlyIncome(income);
        response.setMonthlyExpense(expense);

        // 5. 分類佔比（本月支出，支援 FAMILY 聚合）
        List<CategoryBreakdownItem> breakdown =
                calculateCategoryBreakdown(dashboardTenantIds, firstDayOfMonth, today);

        response.setCategoryBreakdown(breakdown);

        return response;
    }

    /**
     * Dashboard 查詢範圍解析：
     *
     * PERSONAL：
     *   只查目前 tenant。
     *
     * FAMILY：
     *   查 family tenant 自己
     *   + family 成員各自的 PERSONAL tenant。
     */
    @SuppressWarnings("unchecked")
    private List<Long> resolveDashboardTenantIds(Long currentTenantId) {
        if (currentTenantId == null) {
            return Collections.emptyList();
        }

        String tenantType = findTenantType(currentTenantId);

        if (!TENANT_TYPE_FAMILY.equals(tenantType)) {
            return Collections.singletonList(currentTenantId);
        }

        Set<Long> tenantIds = new LinkedHashSet<>();
        tenantIds.add(currentTenantId);

        List<Number> memberUserIdRows = entityManager.createNativeQuery(
                        "SELECT DISTINCT USER_ID " +
                                "FROM user_tenants " +
                                "WHERE TENANT_ID = :familyTenantId " +
                                "  AND STATUS = :status " +
                                "  AND DELETED_AT IS NULL"
                )
                .setParameter("familyTenantId", currentTenantId)
                .setParameter("status", USER_TENANT_STATUS_ACTIVE)
                .getResultList();

        if (memberUserIdRows.isEmpty()) {
            return new ArrayList<>(tenantIds);
        }

        List<Long> memberUserIds = memberUserIdRows.stream()
                .map(Number::longValue)
                .collect(Collectors.toList());

        List<Number> personalTenantRows = entityManager.createNativeQuery(
                        "SELECT DISTINCT t.ID " +
                                "FROM user_tenants ut " +
                                "JOIN tenants t ON t.ID = ut.TENANT_ID " +
                                "WHERE ut.USER_ID IN (:memberUserIds) " +
                                "  AND ut.STATUS = :userTenantStatus " +
                                "  AND ut.DELETED_AT IS NULL " +
                                "  AND t.TYPE = :tenantType " +
                                "  AND t.STATUS = :tenantStatus"
                )
                .setParameter("memberUserIds", memberUserIds)
                .setParameter("userTenantStatus", USER_TENANT_STATUS_ACTIVE)
                .setParameter("tenantType", TENANT_TYPE_PERSONAL)
                .setParameter("tenantStatus", TENANT_STATUS_ACTIVE)
                .getResultList();

        for (Number row : personalTenantRows) {
            tenantIds.add(row.longValue());
        }

        return new ArrayList<>(tenantIds);
    }

    private String findTenantType(Long tenantId) {
        Object result = entityManager.createNativeQuery(
                        "SELECT TYPE " +
                                "FROM tenants " +
                                "WHERE ID = :tenantId " +
                                "  AND STATUS = :status"
                )
                .setParameter("tenantId", tenantId)
                .setParameter("status", TENANT_STATUS_ACTIVE)
                .getSingleResult();

        return result != null ? result.toString() : null;
    }

    private List<UserAccount> loadAccounts(List<Long> tenantIds) {
        List<UserAccount> result = new ArrayList<>();

        for (Long tenantId : tenantIds) {
            List<UserAccount> accounts =
                    userAccountRepository.findByTenantIdAndDeletedAtIsNull(tenantId);

            result.addAll(accounts);
        }

        return result;
    }

    private BigDecimal sumTransactionsByTenantIds(List<Long> tenantIds,
                                                  String txTypeCode,
                                                  LocalDate from,
                                                  LocalDate to) {
        BigDecimal total = BigDecimal.ZERO;

        for (Long tenantId : tenantIds) {
            BigDecimal amount =
                    transactionRepository.sumByTenantIdAndTxTypeCodeAndDateRange(
                            tenantId,
                            txTypeCode,
                            from,
                            to
                    );

            if (amount != null) {
                total = total.add(amount);
            }
        }

        return total;
    }

    private Map<String, BigDecimal> calculateAssetsByTwd(Map<String, BigDecimal> totalAssetsByCurrency) {
        Map<String, BigDecimal> assetsByTwd = new LinkedHashMap<>();

        for (Map.Entry<String, BigDecimal> entry : totalAssetsByCurrency.entrySet()) {
            String currency = entry.getKey();
            BigDecimal amount = entry.getValue();

            if ("TWD".equals(currency)) {
                assetsByTwd.put("TWD", amount);
            } else {
                BigDecimal rate = exchangeRateRepository
                        .findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(currency, "TWD")
                        .map(er -> er.getRate())
                        .orElse(BigDecimal.ONE);

                assetsByTwd.put(
                        currency,
                        amount.multiply(rate).setScale(0, RoundingMode.HALF_UP)
                );
            }
        }

        return assetsByTwd;
    }

    private BigDecimal calculateTotalInTwd(Map<String, BigDecimal> totalAssetsByCurrency) {
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : totalAssetsByCurrency.entrySet()) {
            String currency = entry.getKey();
            BigDecimal amount = entry.getValue();

            if ("TWD".equals(currency)) {
                total = total.add(amount);
            } else {
                BigDecimal rate = exchangeRateRepository
                        .findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(currency, "TWD")
                        .map(er -> er.getRate())
                        .orElse(BigDecimal.ONE);

                total = total.add(amount.multiply(rate).setScale(2, RoundingMode.HALF_UP));
            }
        }

        return total.setScale(0, RoundingMode.HALF_UP);
    }

    private List<CategoryBreakdownItem> calculateCategoryBreakdown(List<Long> tenantIds,
                                                                   LocalDate from,
                                                                   LocalDate to) {
        Map<Long, String> categoryNameMap = loadCategoryNames(tenantIds);
        Map<Long, BigDecimal> categoryAmountMap = new HashMap<>();

        for (Long tenantId : tenantIds) {
            List<Transaction> expenseTransactions = transactionRepository
                    .findByTenantIdAndDateRange(tenantId, from, to)
                    .stream()
                    .filter(t -> "EXPENSE".equals(t.getTxTypeCode()))
                    .collect(Collectors.toList());

            for (Transaction tx : expenseTransactions) {
                List<TransactionItem> items = transactionItemRepository
                        .findByTenantIdAndTransactionId(tenantId, tx.getId());

                for (TransactionItem item : items) {
                    Long categoryId = item.getCategoryId() != null
                            ? item.getCategoryId()
                            : 0L;

                    BigDecimal amount = item.getAmount() != null
                            ? item.getAmount()
                            : BigDecimal.ZERO;

                    categoryAmountMap.merge(categoryId, amount, BigDecimal::add);
                }
            }
        }

        return categoryAmountMap.entrySet().stream()
                .map(e -> {
                    String name = e.getKey() == 0L
                            ? "未分類"
                            : categoryNameMap.getOrDefault(e.getKey(), "分類 #" + e.getKey());

                    return new CategoryBreakdownItem(e.getKey(), name, e.getValue());
                })
                .sorted(Comparator.comparing(CategoryBreakdownItem::getAmount).reversed())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<Long, String> loadCategoryNames(List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT ID, NAME " +
                                "FROM categories " +
                                "WHERE TENANT_ID IN (:tenantIds) " +
                                "  AND IS_ACTIVE = TRUE"
                )
                .setParameter("tenantIds", tenantIds)
                .getResultList();

        Map<Long, String> map = new HashMap<>();

        for (Object[] row : rows) {
            Long id = ((Number) row[0]).longValue();
            String name = (String) row[1];
            map.put(id, name);
        }

        return map;
    }
}