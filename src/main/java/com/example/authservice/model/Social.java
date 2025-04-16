package com.example.authservice.model;

import com.example.authservice.type.Role;
import com.example.authservice.type.Type;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Social {
    private int uid;
    private String userId;
    private String userName;
    private String email;
    private String emailyn;
    private String phone;
    private String phoneyn;
    private String mainAddress;
    private String subAddress1;
    private String subAddress2;
    private Type type;
    private int point;
    private Role role;
    private String status;
    private LocalDateTime createdDate;
}
