package com.example.authservice.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ManagerResponseDTO {
    private Long userUid;
    private String userId;
    private String userName;
    private String assignedStoreName; // 등록된 지점 이름 (없으면 null)
}
