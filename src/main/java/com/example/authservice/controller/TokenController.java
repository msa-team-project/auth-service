package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auths")
public class TokenController {
    private final TokenService tokenService;

    @PostMapping("/refresh")
    public RefreshTokenResponseDTO refreshToken(@RequestBody RefreshRequestDTO refreshRequestDTO) {
        log.info("Refresh token request ");
        return tokenService.refreshToken(refreshRequestDTO.getRefreshToken());
    }

    @PostMapping("/validToken")
    public ValidTokenResponseDTO validToken(@RequestBody ValidTokenRequestDTO validTokenRequestDTO) {
        log.info("Validate token request :: {}", validTokenRequestDTO.getToken());
        return tokenService.validateToken(validTokenRequestDTO.getToken());
    }

    @PostMapping("/claims")
    public ClaimsResponseDTO claims(@RequestBody ClaimsRequestDTO claimsRequestDTO) {
        log.info("Claims request ");
        return tokenService.getAuthentication(claimsRequestDTO.getToken());
    }

    @PostMapping("/re/tokens")
    public RefreshTokenResponseDTO updateTokens(@RequestBody OAuthUpdateTokensDTO oAuthUpdateTokensDTO){
        log.info("Update tokens request ");
        return tokenService.updateTokens(oAuthUpdateTokensDTO.getAccessToken(), oAuthUpdateTokensDTO.getRefreshToken());
    }

}
