package com.example.authservice.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Token {
    private int uid;
    private Integer userUid;
    private Integer socialUid;
    private String accessToken;
    private String refreshToken;
}
