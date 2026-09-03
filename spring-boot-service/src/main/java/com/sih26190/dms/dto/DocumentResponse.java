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

    // Null if no earlier local-watcher record exists for this filename,
    // or if it matched. Non-null only when a mismatch was detected,
    // meaning the file changed between being first seen locally and
    // being uploaded.
    private String preUploadWarning;

}