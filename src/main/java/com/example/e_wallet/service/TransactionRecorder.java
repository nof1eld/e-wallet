package com.example.e_wallet.service;

import com.example.e_wallet.entity.Transaction;
import com.example.e_wallet.event.TransactionCompletedEvent;
import com.example.e_wallet.repository.TransactionRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionRecorder {

    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TransactionRecorder(TransactionRepository transactionRepository, ApplicationEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }
    //  start a new transaction and pause any running one
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedTransaction(Transaction.TransactionType type, Long sourceAccountId,
                              Long destinationAccountId, BigDecimal amount) {
        Transaction failedTs = new Transaction();
        failedTs.setType(type);
        failedTs.setSourceAccountId(sourceAccountId);
        failedTs.setDestinationAccountId(destinationAccountId);
        failedTs.setAmount(amount);
        failedTs.setStatus(Transaction.TransactionStatus.FAILED);
        transactionRepository.save(failedTs);
    }
    
    @Transactional 
    public void recordSucceededTransaction(Transaction.TransactionType type, Long sourceAccountId,
                              Long destinationAccountId, BigDecimal amount) {
        Transaction ts = new Transaction();
        ts.setType(type);
        ts.setSourceAccountId(sourceAccountId);
        ts.setDestinationAccountId(destinationAccountId);
        ts.setAmount(amount);
        ts.setStatus(Transaction.TransactionStatus.SUCCESS);
        transactionRepository.save(ts);
        //  send event to event listener
        eventPublisher.publishEvent(new TransactionCompletedEvent(ts));
    }
}