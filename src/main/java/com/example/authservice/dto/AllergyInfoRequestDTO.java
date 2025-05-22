package com.example.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AllergyInfoRequestDTO {

    @JsonProperty("user_uid")
    private Long userUid;
    private List<String> allergies;

}
