package com.example.e_wallet.service;

import com.example.e_wallet.dto.AccountResponse;
import com.example.e_wallet.entity.Account;
import com.example.e_wallet.repository.AccountRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse getAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found: " + id));
        return AccountResponse.convertFrom(account);
    }
}