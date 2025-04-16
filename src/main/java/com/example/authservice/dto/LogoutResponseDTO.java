package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class LogoutResponseDTO {
    private boolean successed;
}
