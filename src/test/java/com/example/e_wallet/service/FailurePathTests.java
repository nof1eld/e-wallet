package com.example.e_wallet.service;

import com.example.e_wallet.entity.Account;
import com.example.e_wallet.exception.AccountBlockedException;
import com.example.e_wallet.exception.InsufficientBalanceException;
import com.example.e_wallet.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FailurePathTests {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRecorder transactionRecorder;

    @Test
    void withdraw_throwsInsufficientBalance_whenBalanceIsTooLow() {
        AccountService accountService = new AccountService(accountRepository, transactionRecorder);
        Account account = new Account();
        account.setOwnerUsername("alice");
        account.setBalance(new BigDecimal("25.00"));
        account.setStatus(Account.AccountStatus.ACTIVE);

        when(accountRepository.findByIdAndLock(1L)).thenReturn(Optional.of(account));

        InsufficientBalanceException ex = assertThrows(
                InsufficientBalanceException.class,
                () -> accountService.withdraw(1L, new BigDecimal("50.00"))
        );

        assertEquals("Account 1 has insufficient balance for this operation", ex.getMessage());
        verify(transactionRecorder).recordFailedTransaction(
                com.example.e_wallet.entity.Transaction.TransactionType.WITHDRAW,
                1L,
                null,
                new BigDecimal("50.00")
        );
    }

    @Test
    void withdraw_throwsBlockedAccount_whenAccountIsBlocked() {
        AccountService accountService = new AccountService(accountRepository, transactionRecorder);
        Account account = new Account();
        account.setOwnerUsername("bob");
        account.setBalance(new BigDecimal("100.00"));
        account.setStatus(Account.AccountStatus.BLOCKED);

        when(accountRepository.findByIdAndLock(2L)).thenReturn(Optional.of(account));

        AccountBlockedException ex = assertThrows(
                AccountBlockedException.class,
                () -> accountService.withdraw(2L, new BigDecimal("10.00"))
        );

        assertEquals("Account 2 is blocked", ex.getMessage());
        verify(transactionRecorder).recordFailedTransaction(
                com.example.e_wallet.entity.Transaction.TransactionType.WITHDRAW,
                2L,
                null,
                new BigDecimal("10.00")
        );
    }

    @Test
    void transfer_rejectsSelfTransfer() {
        TransferService transferService = new TransferService(accountRepository, transactionRecorder);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(9L, 9L, new BigDecimal("10.00"))
        );

        assertEquals("Cannot transfer to the same account", ex.getMessage());
    }
}
