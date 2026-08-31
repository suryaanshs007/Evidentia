package com.sih26190.dms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String role;
    private String username;

}
