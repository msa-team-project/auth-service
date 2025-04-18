package com.example.authservice.dto;

import com.example.authservice.model.Address;
import com.example.authservice.model.User;
import com.example.authservice.type.Role;
import lombok.Getter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Getter
public class UserJoinRequestDTO {
    private String userId;
    private String password;
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
    private Role role;

    public User toUser(BCryptPasswordEncoder bCryptPasswordEncoder) {
        return User.builder()
                .userId(userId)
                .password(bCryptPasswordEncoder.encode(password))
                .userName(userName)
                .email(email)
                .emailyn(emailyn)
                .phone(phone)
                .phoneyn(phoneyn)
                .role(role)
                .build();
    }

    public Address toAddress() {
        return Address.builder()
                .mainAddress(mainAddress)
                .subAddress1(subAddress1)
                .subAddress2(subAddress2)
                .mainLat(mainLat)
                .mainLan(mainLan)
                .sub1Lat(subLat1)
                .sub1Lan(subLan1)
                .sub2Lat(subLat2)
                .sub2Lan(subLan2)
                .build();
    }
}
