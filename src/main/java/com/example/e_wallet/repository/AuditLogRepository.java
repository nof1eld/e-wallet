package com.example.e_wallet.repository;

import com.example.e_wallet.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository <AuditLog, Long> {

}
