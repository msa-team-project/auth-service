package com.example.authservice.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Token {
    private int uid;
    private int userUid;
    private int socialUid;
    private String accessToken;
    private String refreshToken;
}
