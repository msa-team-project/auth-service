package com.example.authservice.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ManagerDTO {
    private Long userUid;
    private String userId;
    private String userName;
}
