package com.example.e_wallet.service;

import com.example.e_wallet.entity.Account;
import com.example.e_wallet.entity.Transaction;
import com.example.e_wallet.exception.AccountBlockedException;
import com.example.e_wallet.exception.AccountNotFoundException;
import com.example.e_wallet.exception.InsufficientBalanceException;
import com.example.e_wallet.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRecorder transactionRecorder; 


    public TransferService(AccountRepository accountRepository, TransactionRecorder transactionRecorder) {
        this.accountRepository = accountRepository;
        this.transactionRecorder = transactionRecorder;
    }

    @Transactional
    public void transfer(Long sourceId, Long destinationId, BigDecimal amount) {
        
        if (sourceId.equals(destinationId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        //   I lock the account with lowest ID to avoid deadlock when A->B and B->A 
        Long firstToLock = Math.min(sourceId, destinationId);
        Long secondToLock = Math.max(sourceId, destinationId);

        Account first = accountRepository.findByIdAndLock(firstToLock)
                .orElseThrow(() -> new AccountNotFoundException(firstToLock));
        Account second = accountRepository.findByIdAndLock(secondToLock)
                .orElseThrow(() -> new AccountNotFoundException(secondToLock));

        if (first.getStatus() == Account.AccountStatus.BLOCKED) {
            transactionRecorder.recordFailedTransaction(Transaction.TransactionType.TRANSFER, sourceId, destinationId, amount);
            throw new AccountBlockedException(first.getId());
        }
        if (second.getStatus() == Account.AccountStatus.BLOCKED) {
            transactionRecorder.recordFailedTransaction(Transaction.TransactionType.TRANSFER, sourceId, destinationId, amount);
            throw new AccountBlockedException(second.getId());
        }

        //  decide which of the accounts is source and which is destination
        Account source = sourceId.equals(first.getId()) ? first : second;
        Account destination = sourceId.equals(first.getId()) ? second : first;

        if (source.getBalance().compareTo(amount) < 0) {
            transactionRecorder.recordFailedTransaction(Transaction.TransactionType.TRANSFER, sourceId, destinationId, amount);

            throw new InsufficientBalanceException(sourceId);
        }

        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));
        accountRepository.save(source);
        accountRepository.save(destination);

        transactionRecorder.recordSucceededTransaction(Transaction.TransactionType.TRANSFER, sourceId, destinationId, amount);

    }
}