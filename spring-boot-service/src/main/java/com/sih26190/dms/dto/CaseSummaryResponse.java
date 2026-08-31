package com.sih26190.dms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class CaseSummaryResponse {

    private String caseId;
    private String title;
    private String status;
    private long documentCount;

}
