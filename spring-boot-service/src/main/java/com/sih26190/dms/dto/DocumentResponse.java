package com.sih26190.dms.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class DocumentResponse {

    private Long documentId;
    private String caseId;
    private String documentType;
    private String title;
    private String uploadedBy;
    private LocalDateTime uploadedAt;

}
