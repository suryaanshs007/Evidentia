package com.sih26190.dms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// django dashboard requests certain fields that the spring backend doesn't actually provide, so i just used placeholders for now so that the dashboard wont break lol
@Getter
@AllArgsConstructor
public class CaseSummaryResponse {

    private String caseId;
    private String title;
    private String status;
    private long documentCount;

}
