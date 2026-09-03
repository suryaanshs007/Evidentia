package com.sih26190.dms.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidateHashRequest {

    private String filename;
    private String sha256Hash;

}