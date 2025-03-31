package com.example.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name="naverProfileClient", url="https://openapi.naver.com/v1/nid/me")
public interface NaverProfileClient {

    @GetMapping
    String getNaverProfile(@RequestHeader("Authorization")String token);
}
