package com.example.novaledger.finance.transaction.service;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.finance.account.entity.UserAccount;
import com.example.novaledger.finance.account.repository.UserAccountRepository;
import com.example.novaledger.finance.creditcard.entity.UserCreditCard;
import com.example.novaledger.finance.creditcard.repository.CreditCardRepository;
import com.example.novaledger.finance.transaction.entity.Transaction;
import com.example.novaledger.finance.transaction.entity.TransactionItem;
import com.example.novaledger.finance.transaction.repository.TransactionItemRepository;
import com.example.novaledger.finance.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final UserAccountRepository userAccountRepository;
    private final CreditCardRepository creditCardRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            TransactionItemRepository transactionItemRepository,
            UserAccountRepository userAccountRepository,
            CreditCardRepository creditCardRepository) {
        this.transactionRepository = transactionRepository;
        this.transactionItemRepository = transactionItemRepository;
        this.userAccountRepository = userAccountRepository;
        this.creditCardRepository = creditCardRepository;
    }

    @Transactional
    public Transaction createTransaction(Transaction transaction, List<TransactionItem> items) {
        validateSource(transaction);

        Transaction saved = transactionRepository.save(transaction);

        if (items != null && !items.isEmpty()) {
            for (TransactionItem item : items) {
                item.setTenantId(saved.getTenantId());
                item.setTransactionId(saved.getId());
            }
            transactionItemRepository.saveAll(items);
        }

        updateBalance(saved, saved.getTotalAmount(), false);

        return saved;
    }

    @Transactional
    public Transaction updateTransaction(Long transactionId, Long tenantId, Transaction updated, List<TransactionItem> newItems) {
        Transaction existing = transactionRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(transactionId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));

        updateBalance(existing, existing.getTotalAmount(), true);

        existing.setTxTypeCode(updated.getTxTypeCode());
        existing.setTransactionDate(updated.getTransactionDate());
        existing.setTotalAmount(updated.getTotalAmount());
        existing.setCurrencyCode(updated.getCurrencyCode());
        existing.setMemo(updated.getMemo());

        Transaction saved = transactionRepository.save(existing);

        transactionItemRepository.deleteByTransactionId(saved.getId());
        if (newItems != null && !newItems.isEmpty()) {
            for (TransactionItem item : newItems) {
                item.setTenantId(saved.getTenantId());
                item.setTransactionId(saved.getId());
            }
            transactionItemRepository.saveAll(newItems);
        }

        updateBalance(saved, saved.getTotalAmount(), false);

        return saved;
    }

    @Transactional
    public void deleteTransaction(Long transactionId, Long tenantId) {
        Transaction existing = transactionRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(transactionId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));

        updateBalance(existing, existing.getTotalAmount(), true);

        existing.setDeletedAt(LocalDateTime.now());
        transactionRepository.save(existing);
    }

    // ─── private helpers ───────────────────────────────────────────

    private void validateSource(Transaction transaction) {
        boolean hasAccount = transaction.getAccountId() != null;
        boolean hasCard = transaction.getCreditCardId() != null;
        if (hasAccount == hasCard) {
            throw new BusinessException(ErrorCode.TRANSACTION_SOURCE_INVALID);
        }
    }

    private void updateBalance(Transaction transaction, BigDecimal amount, boolean reverse) {
        String txType = transaction.getTxTypeCode();
        int direction = reverse ? -1 : 1;

        if (transaction.getAccountId() != null) {
            UserAccount account = userAccountRepository
                    .findByIdAndTenantIdAndDeletedAtIsNull(transaction.getAccountId(), transaction.getTenantId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_001));

            BigDecimal newBalance = switch (txType) {
                case "INCOME", "TRANSFER_IN" ->
                        account.getCurrentBalance().add(amount.abs().multiply(BigDecimal.valueOf(direction)));
                case "EXPENSE", "TRANSFER_OUT" ->
                        account.getCurrentBalance().subtract(amount.abs().multiply(BigDecimal.valueOf(direction)));
                case "ADJUSTMENT" ->
                        account.getCurrentBalance().add(amount.multiply(BigDecimal.valueOf(direction)));
                default -> account.getCurrentBalance();
            };

            account.setCurrentBalance(newBalance);
            userAccountRepository.save(account);

        } else if (transaction.getCreditCardId() != null) {
            UserCreditCard card = creditCardRepository
                    .findByIdAndTenantIdAndDeletedAtIsNull(transaction.getCreditCardId(), transaction.getTenantId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CARD_001));

            card.setCurrentBalance(card.getCurrentBalance()
                    .add(amount.abs().multiply(BigDecimal.valueOf(direction))));
            creditCardRepository.save(card);
        }
    }
}