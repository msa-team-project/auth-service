package com.example.authservice.service;

import com.example.authservice.client.KakaoTokenClient;
import com.example.authservice.client.NaverProfileClient;
import com.example.authservice.client.NaverTokenClient;
import com.example.authservice.dto.KakaoTokenResponseDTO;
import com.example.authservice.dto.NaverTokenResponseDTO;
import com.example.authservice.dto.NaverUserInfoResponseDTO;
import com.example.authservice.dto.UserLoginResponseDTO;
import com.example.authservice.type.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OAuthService {
    private final NaverTokenClient naverTokenClient;
    private final NaverProfileClient naverProfileClient;

    private final KakaoTokenClient kakaoTokenClient;

    @Value("${oauth.naver.client-id}")
    private String naverClientId;

    @Value("${oauth.naver.client-secret}")
    private String naverClientSecret;

    @Value("${oauth.naver.redirect-uri}")
    private String naverRedirectUri;

    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    public UserLoginResponseDTO getAccessToken(String type, String code, String state, HttpServletResponse response) {
        if(type.equals("naver")){
            NaverTokenResponseDTO responseDTO = naverTokenClient.getAccessToken("authorization_code", naverClientId, naverClientSecret, code, state);
            return UserLoginResponseDTO.builder()
                    .loggedIn(true)
                    .accessToken(type + ":" + responseDTO.getAccess_token())
                    .refreshToken(type + ":" + responseDTO.getRefresh_token())
                    .build();
        }else if(type.equals("kakao")){
            System.out.println("kakao login api request");
            String contentType = "application/x-www-form-urlencoded;charset=utf-8";
            KakaoTokenResponseDTO responseDTO = kakaoTokenClient.getTokens(contentType,"authorization_code",kakaoClientId,kakaoRedirectUri,code);

            System.out.println("accessToken is :: " + responseDTO.getAccess_token());
            System.out.println("refreshToken is :: " + responseDTO.getRefresh_token());

            return UserLoginResponseDTO.builder()
                    .loggedIn(true)
                    .accessToken(type + ":" + responseDTO.getAccess_token())
                    .refreshToken(type + ":" + responseDTO.getRefresh_token())
                    .build();
        }else if (type.equals("google")) {
            System.out.println("google login api request");
            return UserLoginResponseDTO.builder()
                    .loggedIn(true)
                    .accessToken(type + ":" + "testToken")
                    .refreshToken(type + ":" + "testToken")
                    .build();
        }

//        CookieUtil.addCookie(response, "refreshToken",type + ":" + responseDTO.getRefresh_token(), 7*24*60*60 );
//        CookieUtil.addCookie(response, "accessToken",type + ":" + responseDTO.getAccess_token(), 60*60 );
        return null;
    }

    public UserLoginResponseDTO getReAccessToken(String type, String refreshToken, HttpServletResponse response) {

        NaverTokenResponseDTO responseDTO = naverTokenClient.getReAccessToken("refresh_token", naverClientId, naverClientSecret, refreshToken );

//        CookieUtil.addCookie(response, "refreshToken",type + ":" + responseDTO.getRefresh_token(), 7*24*60*60 );
//        CookieUtil.addCookie(response, "accessToken",type + ":" + responseDTO.getAccess_token(), 60*60 );

        return UserLoginResponseDTO.builder()
                .loggedIn(true)
                .accessToken(type + ":" + responseDTO.getAccess_token())
                .refreshToken(type + ":" + responseDTO.getRefresh_token())
                .build();
    }

    public NaverUserInfoResponseDTO getNaverUserInfo(String token) {
        try {
            System.out.println("getNaverUserInfo in token :: "+token);
            String result = naverProfileClient.getNaverProfile("Bearer " + token);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(result);

            return NaverUserInfoResponseDTO.builder()
                    .id(jsonNode.get("response").get("id").asText())
                    .name(jsonNode.get("response").get("name").asText())
                    .nickname(jsonNode.get("response").get("nickname").asText())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Authentication getAuthentication(String token, String name) {
        // Claims에서 역할을 추출하고, GrantedAuthority로 변환
        List<GrantedAuthority> authorities = Collections.singletonList(
                // 권한은 리스트로 여러 개 넣어 줄 수도 있다.
                new SimpleGrantedAuthority(Role.ROLE_USER.name())
        );

        // UserDetails 객체 생성
        UserDetails userDetails = new User(name,"",authorities);

        // spring security에 인증객체 생성한거 등록 해줌 (컨버팅)
        return new UsernamePasswordAuthenticationToken(userDetails, token, authorities);
    }
}
