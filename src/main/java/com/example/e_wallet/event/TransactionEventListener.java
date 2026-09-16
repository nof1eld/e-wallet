package com.example.e_wallet.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.example.e_wallet.entity.AuditLog;
import com.example.e_wallet.repository.AuditLogRepository;


@Component
public class TransactionEventListener {

    private final AuditLogRepository auditLogRepository;

    public TransactionEventListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    //  run on a separate thread
    @Async
    // event listener that waits for transaction to be commited successfully 
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        var ts = event.transaction();

        AuditLog auditLog = new AuditLog();
        auditLog.setTransactionId(ts.getId());
        auditLog.setTransactionType(ts.getType().name());
        auditLog.setSourceAccountId(ts.getSourceAccountId());
        auditLog.setDestinationAccountId(ts.getDestinationAccountId());
        auditLog.setAmount(ts.getAmount());
        auditLog.setStatus(ts.getStatus().name());
        auditLogRepository.save(auditLog);

        // notification message
        System.out.println("Notification sent for transaction "
                + ts.getId() + " (type=" + ts.getType() + ", amount=" + ts.getAmount() + ")");
    }
}