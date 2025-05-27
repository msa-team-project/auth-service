package com.example.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UpdateAllergyRequestDTO {
    @JsonProperty("user_uid")
    private Integer userUid;
    @JsonProperty("social_uid")
    private Integer socialUid;
    private List<String> allergies;
}
