package com.example.authservice.model;

import com.example.authservice.type.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class User {
    private int uid;
    private String userId;
    private String password;
    private String userName;
    private String email;
    private String emailyn;
    private String phone;
    private String phoneyn;
    private int point;
    private Role role;
    private String status;
    private LocalDateTime createdDate;
}
