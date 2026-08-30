package com.sih26190.dms.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.sih26190.dms.model.AuditLog;
import com.sih26190.dms.model.DocumentRecord;
import com.sih26190.dms.model.User;
import com.sih26190.dms.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(User user, DocumentRecord document, String action) {
        AuditLog entry = new AuditLog();
        entry.setUser(user);
        entry.setDocument(document);
        entry.setAction(action);
        entry.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(entry);
    }

}
