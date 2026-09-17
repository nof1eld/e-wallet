package com.example.e_wallet.config;

import com.example.e_wallet.repository.AccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;


@Component
public class AccountSecurity {

    private final AccountRepository accountRepository;

    public AccountSecurity(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // For READ access, for admin or account owner
    public boolean canRead(Long accountId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        return isOwner(accountId, authentication);
    }

    // For WRITE access, account owner only
    public boolean canWrite(Long accountId, Authentication authentication) {
        return isOwner(accountId, authentication);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));
    }

    private boolean isOwner(Long accountId, Authentication authentication) {
        return accountRepository.findById(accountId)
                .map(account -> account.getOwnerUsername().equals(authentication.getName()))
                .orElse(false);
    }
}