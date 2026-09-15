package com.example.e_wallet.service;

import com.example.e_wallet.entity.Account;
import com.example.e_wallet.entity.Transaction;
import com.example.e_wallet.exception.InsufficientBalanceException;
import com.example.e_wallet.repository.AccountRepository;
import com.example.e_wallet.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransferService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void transfer(Long sourceId, Long destinationId, BigDecimal amount) {

        //   I lock the account with lowest ID to avoid deadlock when A->B and B->A 
        Long firstToLock = Math.min(sourceId, destinationId);
        Long secondToLock = Math.max(sourceId, destinationId);

        Account first = accountRepository.findByIdAndLock(firstToLock)
                .orElseThrow(() -> new RuntimeException("Account not found: " + firstToLock));
        Account second = accountRepository.findByIdAndLock(secondToLock)
                .orElseThrow(() -> new RuntimeException("Account not found: " + secondToLock));

        //  decide which of the accounts is source and which is destination
        Account source = sourceId.equals(first.getId()) ? first : second;
        Account destination = sourceId.equals(first.getId()) ? second : first;

        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(sourceId);
        }

        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));
        accountRepository.save(source);
        accountRepository.save(destination);

        Transaction ts = new Transaction();
        ts.setType(Transaction.TransactionType.TRANSFER);
        ts.setSourceAccountId(sourceId);
        ts.setDestinationAccountId(destinationId);
        ts.setAmount(amount);
        ts.setStatus(Transaction.TransactionStatus.SUCCESS);
        transactionRepository.save(ts);
    }
}