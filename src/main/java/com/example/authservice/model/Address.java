package com.example.authservice.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Address {
    private Long uid;
    private Integer userUid;
    private Integer socialUid;
    private String mainAddress;
    private String subAddress1;
    private String subAddress2;
    //이하는 주소들을 x,y값으로 변환한 필드
    private double mainLat;
    private double mainLan;
    private double sub1Lat;
    private double sub1Lan;
    private double sub2Lat;
    private double sub2Lan;
}
