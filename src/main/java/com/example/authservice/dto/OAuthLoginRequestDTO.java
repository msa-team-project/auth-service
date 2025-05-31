package com.example.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLoginRequestDTO {
    private String accessToken;
    private String refreshToken;
    private String id;
    private String name;
    private String nickname;
    private String email;
    private String mobile;
}
