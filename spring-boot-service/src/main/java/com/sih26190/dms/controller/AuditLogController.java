package com.sih26190.dms.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sih26190.dms.dto.AuditLogResponse;
import com.sih26190.dms.model.AuditLog;
import com.sih26190.dms.model.DocumentRecord;
import com.sih26190.dms.repository.AuditLogRepository;
import com.sih26190.dms.repository.DocumentRecordRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final DocumentRecordRepository documentRecordRepository;

    @GetMapping
    public List<AuditLogResponse> list(
            @RequestParam(required = false) String caseId,
            @RequestParam(required = false) Long documentId) {

        List<AuditLog> entries;

        if (documentId != null) {
            DocumentRecord document = documentRecordRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            entries = auditLogRepository.findByDocument(document);
        } else if (caseId != null) {
            entries = documentRecordRepository.findByCaseId(caseId).stream()
                    .flatMap(doc -> auditLogRepository.findByDocument(doc).stream())
                    .toList();
        } else {
            entries = auditLogRepository.findAll();
        }

        return entries.stream()
                .map(this::toResponse)
                .sorted((a, b) -> b.getPerformedAt().compareTo(a.getPerformedAt()))
                .toList();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getDocument().getId(),
                log.getDocument().getCaseId(),
                log.getAction(),
                log.getUser().getUsername(),
                log.getTimestamp()
        );
    }

}
