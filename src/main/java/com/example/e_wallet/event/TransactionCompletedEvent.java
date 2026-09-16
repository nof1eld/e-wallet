package com.example.e_wallet.event;

import com.example.e_wallet.entity.Transaction;

public record TransactionCompletedEvent(Transaction transaction) {
}