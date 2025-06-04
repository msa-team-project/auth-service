package com.example.authservice.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateAddressRequestDTO {
    private Long userUid;
    private Long socialUid;
    private String mainAddress;
    private String subAddress1;
    private String subAddress2;
    private double mainLat;
    private double mainLan;
    private double subLat1;
    private double subLan1;
    private double subLat2;
    private double subLan2;
}
