package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UpdateProfileRequestDTO {
    private int uid;
    private String userName;
    private String email;
    private String emailyn;
    private String phone;
    private String phoneyn;
    private String mainAddress;
    private String subAddress1;
    private String subAddress2;
    private double mainLat;
    private double mainLan;
    private double subLat1;
    private double subLan1;
    private double subLat2;
    private double subLan2;
    private List<String> allergies;
}
