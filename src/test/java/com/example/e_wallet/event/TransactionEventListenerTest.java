package com.example.e_wallet.event;

import com.example.e_wallet.entity.Account;
import com.example.e_wallet.entity.AuditLog;
import com.example.e_wallet.repository.AccountRepository;
import com.example.e_wallet.repository.AuditLogRepository;
import com.example.e_wallet.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TransactionEventListenerTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void auditLogOnlyCreatedForSuccessfulCommittedTransaction() throws InterruptedException {
        Account account = new Account();
        account.setOwnerUsername("audit-user");
        account.setBalance(new BigDecimal("0.00"));
        account.setStatus(Account.AccountStatus.ACTIVE);
        account = accountRepository.save(account);

        long beforeDeposit = auditLogRepository.count();

        accountService.deposit(account.getId(), new BigDecimal("50.00"));

        Long accountId = account.getId();
        int attempts = 0;
    
        // the audit log runs on a separate thread, to make sure we catch it properly, we try up to 20 times
        while (attempts < 20) {
            
            List<AuditLog> logs = auditLogRepository.findAll();
            if (logs.size() > beforeDeposit) {
                
                // we look for any row in the audit table that has the following values 
                assertThat(logs).anySatisfy(log -> {
                    assertThat(log.getTransactionType()).isEqualTo("DEPOSIT");
                    assertThat(log.getSourceAccountId()).isNull();
                    assertThat(log.getDestinationAccountId()).isEqualTo(accountId);
                    assertThat(log.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
                    assertThat(log.getStatus()).isEqualTo("SUCCESS");
                });
                return;
            }
            // wait before checking again
            Thread.sleep(100);
            attempts++;
        }

        throw new AssertionError("Audit log was not created for a successful committed deposit");
    }

    @Test
    void failedTransactionDoesNotGenerateAuditLogEntry() {
        Account account = new Account();
        account.setOwnerUsername("failed-audit-user");
        account.setBalance(new BigDecimal("10.00"));
        account.setStatus(Account.AccountStatus.ACTIVE);
        account = accountRepository.save(account);

        long beforeWithdraw = auditLogRepository.count();
        try {
            accountService.withdraw(account.getId(), new BigDecimal("25.00"));
        } catch (RuntimeException ignored) {
            // expected: insufficient balance should fail without audit event
        }

        assertThat(auditLogRepository.count()).isEqualTo(beforeWithdraw);
    }
}
