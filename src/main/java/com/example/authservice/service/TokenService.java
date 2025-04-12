package com.example.authservice.service;

import com.example.authservice.dto.ClaimsResponseDTO;
import com.example.authservice.dto.RefreshTokenResponseDTO;
import com.example.authservice.dto.ValidTokenResponseDTO;
import com.example.authservice.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenProviderService tokenProviderService;

    public RefreshTokenResponseDTO refreshToken(String refreshToken) {
        int result = tokenProviderService.validToken("refreshToken", refreshToken);
        // oauth와 자체가입자의 로직 구분해야함
        // oauth 토큰 재발행을 프론트에서 해야함

        String newAccessToken = null;
        String newRefreshToken = null;

        if(result == 1) {
            User user = tokenProviderService.getTokenDetails(refreshToken);

            newAccessToken = tokenProviderService.generateToken(user, Duration.ofHours(2));
            newRefreshToken = tokenProviderService.generateToken(user, Duration.ofDays(2));
        }

        return RefreshTokenResponseDTO.builder()
                .status(result)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public ClaimsResponseDTO getAuthentication(String token) {
        return tokenProviderService.getAuthentication(token);
    }

    public ValidTokenResponseDTO validateToken(String token) {
        int result = tokenProviderService.validToken("accessToken", token);
        return ValidTokenResponseDTO.builder()
                .statusNum(result)
                .build();
    }
}
