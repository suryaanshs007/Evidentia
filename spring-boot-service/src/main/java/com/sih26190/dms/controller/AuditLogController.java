package com.sih26190.dms.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sih26190.dms.dto.AuditLogResponse;
import com.sih26190.dms.model.AuditLog;
import com.sih26190.dms.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public List<AuditLogResponse> list(
            @RequestParam(required = false) String caseId,
            @RequestParam(required = false) Long documentId) {

        List<AuditLog> entries;

        if (documentId != null) {
            entries = auditLogRepository.findByDocumentId(documentId);
        } else if (caseId != null) {
            entries = auditLogRepository.findByCaseId(caseId);
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
                log.getDocumentId(),
                log.getCaseId(),
                log.getAction(),
                log.getUser().getUsername(),
                log.getTimestamp()
        );
    }

}
