package com.example.authservice.dto;

import com.example.authservice.model.Address;
import com.example.authservice.model.User;
import com.example.authservice.type.Role;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

@Getter
public class UserJoinRequestDTO {
    private String userId;
    private String password;
    private String userName;
    private List<String> allergies;
    private String email;
    private String emailyn;
    @Size(min = 11, message = "전화번호는 최소 11자리 이상이어야 합니다.")
    private String phone;
    private String phoneyn;
    private String mainAddress;
    private double mainLat;
    private double mainLan;
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
                .mainLat(mainLat)
                .mainLan(mainLan)
                .build();
    }
}
