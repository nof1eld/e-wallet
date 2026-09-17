package com.example.e_wallet.service;

import com.example.e_wallet.dto.AccountResponse;
import com.example.e_wallet.entity.Account;
import com.example.e_wallet.repository.AccountRepository;
import org.springframework.stereotype.Service;
import com.example.e_wallet.entity.Transaction;
import com.example.e_wallet.exception.AccountBlockedException;
import com.example.e_wallet.exception.AccountNotFoundException;
import com.example.e_wallet.exception.InsufficientBalanceException;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRecorder transactionRecorder; 


    public AccountService(AccountRepository accountRepository, TransactionRecorder transactionRecorder) {
        this.accountRepository = accountRepository;
        this.transactionRecorder = transactionRecorder;
    }

    public AccountResponse getAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        return AccountResponse.convertFrom(account);
    }

    @Transactional
    public void deposit(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdAndLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (account.getStatus() == Account.AccountStatus.BLOCKED) {
            transactionRecorder.recordFailedTransaction(Transaction.TransactionType.DEPOSIT, null, accountId, amount);
            throw new AccountBlockedException(accountId);
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        transactionRecorder.recordSucceededTransaction(Transaction.TransactionType.DEPOSIT, null, accountId, amount);

    }

    @Transactional
    public void withdraw(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdAndLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (account.getStatus() == Account.AccountStatus.BLOCKED) {
            transactionRecorder.recordFailedTransaction(Transaction.TransactionType.WITHDRAW, accountId, null, amount);
            throw new AccountBlockedException(accountId);
        }

        if (account.getBalance().compareTo(amount) < 0) {
            transactionRecorder.recordFailedTransaction(Transaction.TransactionType.WITHDRAW, accountId, null, amount);
            throw new InsufficientBalanceException(accountId);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        transactionRecorder.recordSucceededTransaction(Transaction.TransactionType.WITHDRAW, accountId, null, amount);
    }
}