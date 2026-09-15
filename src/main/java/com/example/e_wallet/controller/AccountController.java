package com.example.e_wallet.controller;

import com.example.e_wallet.dto.AccountResponse;
import com.example.e_wallet.service.AccountService;
import com.example.e_wallet.dto.DepositRequest;
import com.example.e_wallet.dto.WithdrawRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@RestController

@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable Long id) {
        return accountService.getAccount(id);
    }

    @PostMapping("/{id}/deposit")
    public void deposit(@PathVariable Long id, @Valid @RequestBody DepositRequest request) {
        accountService.deposit(id, request.amount());
    }

    @PostMapping("/{id}/withdraw")
    public void withdraw(@PathVariable Long id, @Valid @RequestBody WithdrawRequest request) {
        accountService.withdraw(id, request.amount());
    }
}