package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AllergyInfoRequestDTO {
    private Long userUid;
    private List<String> allergies;
}
