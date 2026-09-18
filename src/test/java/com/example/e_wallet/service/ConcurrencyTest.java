package com.example.e_wallet.service;

import com.example.e_wallet.entity.Account;
import com.example.e_wallet.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransferService transferService;

    @Test
    void concurrentWithdrawalsNeverOverdraw() throws InterruptedException {
        Account account = new Account();
        account.setOwnerUsername("test-user-1");
        account.setBalance(new BigDecimal("100"));
        account = accountRepository.save(account);
        Long accountId = account.getId();

        //  we create a countdown to force both threads to wait at same instance
        CountDownLatch readyLatch = new CountDownLatch(2);

        //  we create a countdown to force both threads to run at same instance
        CountDownLatch startLatch = new CountDownLatch(1);

        Runnable withdrawTask = () -> {
            //  will be 0 after the two threads execute it, allowing the main thread to continue
            readyLatch.countDown();
            try {
                //  wait for main thread to decrement the startLatch
                startLatch.await();
                accountService.withdraw(accountId, new BigDecimal("80.00"));
            } catch (Exception e) {
                //  we expect one of the threads to throw a "insufficient balance exception"
            }
        };

        //  create two threads to run the withdraw test
        Thread thread1 = new Thread(withdrawTask);
        Thread thread2 = new Thread(withdrawTask);

        thread1.start();
        thread2.start();

        //  main thread will wait here after it created the two threads, until readyLatch becomes 0
        readyLatch.await();
        // the two threads will continue
        startLatch.countDown();

        // wait for both threads to finish
        thread1.join();
        thread2.join();

        Account refreshedAccount = accountRepository.findById(accountId).orElseThrow();

        assertEquals(0, new BigDecimal("20").compareTo(refreshedAccount.getBalance()),
                "Expected exactly one withdrawal to succeed (100 - 80 = 20), got " + refreshedAccount.getBalance());
    }

    @Test
    void concurrentOppositeTransfersDoNotDeadlock() throws InterruptedException {
        // create two accounts with 100 balance, we'll execute A->B and B->A at the same time
        Account accountA = new Account();
        accountA.setOwnerUsername("test-user-a");
        accountA.setBalance(new BigDecimal("100"));
        accountA = accountRepository.save(accountA);

        Account accountB = new Account();
        accountB.setOwnerUsername("test-user-b");
        accountB.setBalance(new BigDecimal("100"));
        accountB = accountRepository.save(accountB);

        Long idA = accountA.getId();
        Long idB = accountB.getId();

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Runnable transferAtoB = () -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                transferService.transfer(idA, idB, new BigDecimal("30"));
            } catch (Exception e) {
                // not expected to fail here
            }
        };

        Runnable transferBtoA = () -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                transferService.transfer(idB, idA, new BigDecimal("20"));
            } catch (Exception e) {
            }
        };

        Thread thread1 = new Thread(transferAtoB);
        Thread thread2 = new Thread(transferBtoA);
        thread1.start();
        thread2.start();

        readyLatch.await();
        startLatch.countDown();

        // wait up to 10 seconds, in case of a deadlock
        thread1.join(10000);
        thread2.join(10000);

        // if 10 seconds pass and isAlive is true, then we have a deadlock
        assertTrue(!thread1.isAlive() && !thread2.isAlive(),
                "Transfers did not complete within defined time, likely a deadlock");

        Account refreshedAccountA = accountRepository.findById(idA).orElseThrow();
        Account refreshedAccountB = accountRepository.findById(idB).orElseThrow();

        // A: 100 - 30 (sent to B) + 20 (received from B) = 90
        // B: 100 + 30 (received from A) - 20 (sent to A) = 110
        assertEquals(0, new BigDecimal("90.00").compareTo(refreshedAccountA.getBalance()));
        assertEquals(0, new BigDecimal("110.00").compareTo(refreshedAccountB.getBalance()));
    }
}