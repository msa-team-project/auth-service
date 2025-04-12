package com.example.authservice.dto;

import com.example.authservice.type.Role;
import com.example.authservice.type.Type;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OAuthLoginResponseDTO {
    private boolean loggedIn;
    private Type type;
    private String userName;
    private String email;
    private String mobile;
    private Role role;
    private String accessToken;
    private String refreshToken;
    private String message;
}
