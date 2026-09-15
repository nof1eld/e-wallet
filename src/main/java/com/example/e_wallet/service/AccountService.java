package com.example.e_wallet.service;

import com.example.e_wallet.dto.AccountResponse;
import com.example.e_wallet.entity.Account;
import com.example.e_wallet.repository.AccountRepository;
import org.springframework.stereotype.Service;
import com.example.e_wallet.entity.Transaction;
import com.example.e_wallet.exception.InsufficientBalanceException;
import com.example.e_wallet.repository.TransactionRepository;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public AccountResponse getAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found: " + id));
        return AccountResponse.convertFrom(account);
    }

    @Transactional
    public void deposit(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdAndLock(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction ts = new Transaction();
        ts.setType(Transaction.TransactionType.DEPOSIT);
        ts.setDestinationAccountId(accountId);
        ts.setAmount(amount);
        ts.setStatus(Transaction.TransactionStatus.SUCCESS);
        transactionRepository.save(ts);
    }

    @Transactional
    public void withdraw(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdAndLock(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(accountId);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction ts = new Transaction();
        ts.setType(Transaction.TransactionType.WITHDRAW);
        ts.setSourceAccountId(accountId);
        ts.setAmount(amount);
        ts.setStatus(Transaction.TransactionStatus.SUCCESS);
        transactionRepository.save(ts);
    }
}