package com.sih26190.dms.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuditLogResponse {

    private Long auditId;
    private Long documentId;
    private String caseId;
    private String action;
    private String performedBy;
    private LocalDateTime performedAt;

}
