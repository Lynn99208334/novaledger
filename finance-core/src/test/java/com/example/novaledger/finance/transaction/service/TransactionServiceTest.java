package com.example.novaledger.finance.transaction.service;

import com.example.novaledger.common.exception.BusinessException;
import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.finance.account.entity.UserAccount;
import com.example.novaledger.finance.account.repository.UserAccountRepository;
import com.example.novaledger.finance.creditcard.repository.CreditCardRepository;
import com.example.novaledger.finance.transaction.entity.Transaction;
import com.example.novaledger.finance.transaction.repository.TransactionItemRepository;
import com.example.novaledger.finance.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionItemRepository transactionItemRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private CreditCardRepository creditCardRepository;

    @InjectMocks
    private TransactionService transactionService;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long ACCOUNT_ID = 100L;
    private static final Long TRANSACTION_ID = 200L;

    // ─── createTransaction ─────────────────────────────────────────

    @Test
    void should_create_transaction_and_deduct_balance_when_expense() {
        // arrange
        Transaction tx = buildTransaction("EXPENSE", BigDecimal.valueOf(500), ACCOUNT_ID, null);

        UserAccount account = new UserAccount();
        account.setCurrentBalance(BigDecimal.valueOf(1000));

        when(transactionRepository.save(any())).thenReturn(tx);
        when(userAccountRepository.findByIdAndTenantIdAndDeletedAtIsNull(ACCOUNT_ID, TENANT_ID))
                .thenReturn(Optional.of(account));

        // act
        transactionService.createTransaction(tx, List.of());

        // assert
        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void should_create_transaction_and_add_balance_when_income() {
        // arrange
        Transaction tx = buildTransaction("INCOME", BigDecimal.valueOf(300), ACCOUNT_ID, null);

        UserAccount account = new UserAccount();
        account.setCurrentBalance(BigDecimal.valueOf(1000));

        when(transactionRepository.save(any())).thenReturn(tx);
        when(userAccountRepository.findByIdAndTenantIdAndDeletedAtIsNull(ACCOUNT_ID, TENANT_ID))
                .thenReturn(Optional.of(account));

        // act
        transactionService.createTransaction(tx, List.of());

        // assert
        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(1300));
    }

    // ─── deleteTransaction ─────────────────────────────────────────

    @Test
    void should_set_deleted_at_and_restore_balance_when_delete_expense_transaction() {
        // arrange
        Transaction tx = buildTransaction("EXPENSE", BigDecimal.valueOf(500), ACCOUNT_ID, null);
        tx.setId(TRANSACTION_ID);

        UserAccount account = new UserAccount();
        account.setCurrentBalance(BigDecimal.valueOf(500));

        when(transactionRepository.findByIdAndTenantIdAndDeletedAtIsNull(TRANSACTION_ID, TENANT_ID))
                .thenReturn(Optional.of(tx));
        when(userAccountRepository.findByIdAndTenantIdAndDeletedAtIsNull(ACCOUNT_ID, TENANT_ID))
                .thenReturn(Optional.of(account));
        when(transactionRepository.save(any())).thenReturn(tx);

        // act
        transactionService.deleteTransaction(TRANSACTION_ID, TENANT_ID);

        // assert balance restored
        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getCurrentBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(1000));

        // assert soft delete
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void should_throw_when_transaction_not_found_on_delete() {
        // arrange
        when(transactionRepository.findByIdAndTenantIdAndDeletedAtIsNull(TRANSACTION_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> transactionService.deleteTransaction(TRANSACTION_ID, TENANT_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TRANSACTION_NOT_FOUND));
    }

    // ─── helper ────────────────────────────────────────────────────

    private Transaction buildTransaction(String txTypeCode, BigDecimal amount, Long accountId, Long creditCardId) {
        Transaction tx = new Transaction();
        tx.setTenantId(TENANT_ID);
        tx.setUserId(USER_ID);
        tx.setTxTypeCode(txTypeCode);
        tx.setTotalAmount(amount);
        tx.setCurrencyCode("TWD");
        tx.setTransactionDate(LocalDate.now());
        tx.setAccountId(accountId);
        tx.setCreditCardId(creditCardId);
        return tx;
    }
}