package com.example.e_wallet.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(Long accountId) {
        super("Account " + accountId + " has insufficient balance for this operation");
    }
}