package com.example.sanitationassessment.dto.auth;

import com.example.sanitationassessment.dto.user.SystemUserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private SystemUserResponse user;
}
