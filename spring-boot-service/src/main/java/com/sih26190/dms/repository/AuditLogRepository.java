package com.sih26190.dms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sih26190.dms.model.AuditLog;
import com.sih26190.dms.model.DocumentRecord;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByDocument(DocumentRecord document);

}
