package com.example.authservice.service;

import com.example.authservice.config.jwt.JwtProperties;
import com.example.authservice.dto.ClaimsResponseDTO;
import com.example.authservice.model.User;
import com.example.authservice.type.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static io.jsonwebtoken.Header.JWT_TYPE;
import static io.jsonwebtoken.Header.TYPE;
import static io.jsonwebtoken.SignatureAlgorithm.HS512;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenProviderService {
    private final JwtProperties jwtProperties;

    public String generateToken(User user, Duration expiration) {
        Date now = new Date();

        return makeToken(
                new Date(now.getTime() + expiration.toMillis()),
                user
        );
    }

    public int validToken(String token) {
        try{
            Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return 1;
        }catch (ExpiredJwtException e){
            // 토큰이 만료된 경우
            log.info("Token이 만료되었습니다.");
            return 2;
        }catch (Exception e){
            // 복호화 과정에서 에러가 나면 유효하지 않은 토큰
            System.out.println("Token 복호화 에러 : " + e.getMessage());
            return 3;
        }
    }

    public User getTokenDetails(String token) {
        Claims claims = getClaims(token);
        return User.builder()
                .uid(claims.get("uid", Long.class))
                .userId(claims.getSubject())
                .userName(claims.get("userName", String.class))
                .role(Role.valueOf(claims.get("role",String.class)))
                .build();
    }

    public ClaimsResponseDTO getAuthentication(String token) {
        Claims claims = getClaims(token);
        return ClaimsResponseDTO.builder()
                .userId(claims.getSubject())
                .roles(List.of(claims.get("role", String.class)))
                .build();
    }

    private String makeToken(Date expire, User user) {
        Date now = new Date();

        return Jwts.builder()
                .setHeaderParam(TYPE, JWT_TYPE)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(now)
                .setExpiration(expire)
                .setSubject(user.getUserId())
                .claim("uid", user.getUid())
                .claim("role",user.getRole().name())
                .claim("userName",user.getUserName())
                .signWith(getSecretKey(), HS512)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
