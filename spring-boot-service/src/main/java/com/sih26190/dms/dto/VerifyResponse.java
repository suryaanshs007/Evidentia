package com.sih26190.dms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VerifyResponse {
    private Long documentId;
    private boolean tampered;
    private String message;
}
