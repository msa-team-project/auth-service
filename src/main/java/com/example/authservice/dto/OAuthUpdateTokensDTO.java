package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthUpdateTokensDTO {
    private String accessToken;
    private String refreshToken;
}

