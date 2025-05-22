package com.example.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class UserJoinResponseDTO {
    @JsonProperty("success")
    private boolean isSuccess;
    private String message;
    private int userUid;
}
