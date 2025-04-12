package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthLoginRequestDTO {
    private String accessToken;
    private String refreshToken;
    private String id;
    private String name;
    private String nickname;
    private String email;
    private String mobile;
}
