package com.example.e_wallet.controller;

import com.example.e_wallet.dto.AccountResponse;
import com.example.e_wallet.service.AccountService;
import com.example.e_wallet.dto.DepositRequest;
import com.example.e_wallet.dto.WithdrawRequest;

import org.springframework.security.access.prepost.PreAuthorize;
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

    // check if requester is allowed to use this method with specified id, (read only)
    @PreAuthorize("@accountSecurity.canRead(#id, authentication)")
    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable Long id) {
        return accountService.getAccount(id);
    }

    @PreAuthorize("@accountSecurity.canWrite(#id, authentication)")
    @PostMapping("/{id}/deposit")
    public void deposit(@PathVariable Long id, @Valid @RequestBody DepositRequest request) {
        accountService.deposit(id, request.amount());
    }

    @PreAuthorize("@accountSecurity.canWrite(#id, authentication)")
    @PostMapping("/{id}/withdraw")
    public void withdraw(@PathVariable Long id, @Valid @RequestBody WithdrawRequest request) {
        accountService.withdraw(id, request.amount());
    }
}