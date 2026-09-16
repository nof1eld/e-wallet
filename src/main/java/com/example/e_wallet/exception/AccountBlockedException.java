package com.example.e_wallet.exception;

public class AccountBlockedException extends RuntimeException {
    public AccountBlockedException(Long accountId) {
        super("Account " + accountId + " is blocked");
    }
}
