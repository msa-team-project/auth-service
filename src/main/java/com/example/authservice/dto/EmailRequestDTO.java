package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
//이메일 인증 시 사용
public class EmailRequestDTO {
    private String code;
}
