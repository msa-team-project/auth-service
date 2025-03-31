package com.example.authservice.controller;

import com.example.authservice.dto.UserLoginResponseDTO;
import com.example.authservice.service.OAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth")
public class OAuthController {
    private final OAuthService oAuthService;

    @GetMapping("/naver/callback")
    public UserLoginResponseDTO naverCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response
    ) throws IOException {
        // 'code'는 네이버에서 받은 인증 코드입니다.
        System.out.println("naverCallback " + code + " " + state);

        UserLoginResponseDTO responseDTO = oAuthService.getAccessToken("naver",code, state, response);

        // token 받아오는거 까지 완료됨
        // responseData 어디로 던져줄지 수정해야함
        // openFeign쓰던지 기존에 빈페이지로 던져서 처리하던지
        response.sendRedirect("http://localhost:9000/oauth/token");

        return responseDTO;
        // 사용자 정보를 사용하여 로그인 처리, 회원가입 등을 진행
        // 예를 들어, 로그인 처리를 하고 메인 페이지로 리디렉션
    }

    @GetMapping("/kakao/callback")
    public UserLoginResponseDTO kakaoCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response
    ) throws IOException {
        System.out.println("kakaoCallback " + code + " " + state);

        UserLoginResponseDTO responseDTO = oAuthService.getAccessToken("kakao",code, state, response);

        // token 받아오는거 까지 완료됨
        // responseData 어디로 던져줄지 수정해야함
        // openFeign쓰던지 기존에 빈페이지로 던져서 처리하던지
        response.sendRedirect("http://localhost:9000/oauth/token");

        return responseDTO;
        // 사용자 정보를 사용하여 로그인 처리, 회원가입 등을 진행
        // 예를 들어, 로그인 처리를 하고 메인 페이지로 리디렉션
    }

    @GetMapping("/google/callback")
    public UserLoginResponseDTO googleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response
    ) throws IOException {
        System.out.println("googleCallback " + code + " " + state);

        UserLoginResponseDTO responseDTO = oAuthService.getAccessToken("google",code, state, response);

        // token 받아오는거 까지 완료됨
        // responseData 어디로 던져줄지 수정해야함
        // openFeign쓰던지 기존에 빈페이지로 던져서 처리하던지
        response.sendRedirect("http://localhost:9000/oauth/token");

        return responseDTO;
        // 사용자 정보를 사용하여 로그인 처리, 회원가입 등을 진행
        // 예를 들어, 로그인 처리를 하고 메인 페이지로 리디렉션
    }
}
