package com.sih26190.dms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;


//  if this project moves to
//  JWT later, this is the class that changes shape, callers of the
//  login endpoint should not otherwise be affected.

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String role;
    private String username;

}
