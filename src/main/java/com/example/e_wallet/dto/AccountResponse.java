package com.example.e_wallet.dto;

import com.example.e_wallet.entity.Account;
import java.math.BigDecimal;

public record AccountResponse(Long id, String ownerUsername, BigDecimal balance, String status) {


    public static AccountResponse convertFrom(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwnerUsername(),
                account.getBalance(),
                account.getStatus().name()
        );
    }
}