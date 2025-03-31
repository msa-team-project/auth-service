package com.example.authservice.dto;

import lombok.Getter;

@Getter
public class UserLoginRequestDTO {
    private String userId;
    private String password;
}
