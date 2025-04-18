package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateAddressRequestDTO {
    private String mainAddress;
    private String subAddress1;
    private String subAddress2;
}
