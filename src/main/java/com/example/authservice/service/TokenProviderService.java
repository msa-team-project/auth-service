package com.example.authservice.service;

import com.example.authservice.config.jwt.JwtProperties;
import com.example.authservice.dto.ClaimsResponseDTO;
import com.example.authservice.mapper.TokenMapper;
import com.example.authservice.model.Token;
import com.example.authservice.model.User;
import com.example.authservice.type.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TokenMapper tokenMapper;
    private final StringRedisTemplate redisTemplate;

    public String generateToken(User user, Duration expiration) {
        Date now = new Date();

        return makeToken(
                new Date(now.getTime() + expiration.toMillis()),
                user
        );
    }

    public int validToken(String tokenType, String token) {
        // 소셜로그인 토큰인지 사이트 자체 토큰인지 구분하는 코드가 필요함
        // 소셜로그인 토큰이라면 redis에 해당 토큰있는지 찾아보고 있다면 DB에서 이름으로 사용자 찾아야함
        // 토큰만받는다면 결국에 그 토큰이 누구껀지 모름
        // accessToken이나 refreshToken 재발급시 DB에도 해당 사항 변경해야함
        // 자체 로그인이면 기존 로직대로 수행하면 됨
        String[] splitTokenArr = token.split(":");

        if(splitTokenArr == null){
            log.info("Token이 존재하지 않습니다.");
            return 0;
        }else{
            // 일단 redis 확인하는거는 확정
            // DB도 확인하는거는 보류 (DB에 토큰 저장없이 고려 중)
            if(
                    splitTokenArr[0].equals("naver") ||
                            splitTokenArr[0].equals("kakao") ||
                            splitTokenArr[0].equals("google")
            ){
                String findTokenFromRedis;
                
                if(tokenType.equals("accessToken")){
                    findTokenFromRedis = getAccessTokenFromRedis(splitTokenArr[1]);
                }else if(tokenType.equals("refreshToken")){
                    findTokenFromRedis = getRefreshTokenFromRedis(splitTokenArr[1]);
                }else{
                    findTokenFromRedis = null;
                }

                if(findTokenFromRedis == null){
                    return 2;
                }else if(findTokenFromRedis.equals(splitTokenArr[2])){
                    return 1;
                }else{
                    return 3;
                }
            }else{
                // 기존의 자체 사이트 가입자 토큰 검증 로직
                // redis에서 토큰 찾는 로직 추가해야 함
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
        }
    }

    public User getTokenDetails(String token) {
        Claims claims = getClaims(token);
        return User.builder()
                .uid(claims.get("uid", Integer.class))
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

    // Redis에 accessToken과 refreshToken을 저장
    public void saveTokensToRedis(String userId, String accessToken, String refreshToken) {
        // accessToken 저장 + 2시간 만료
        redisTemplate.opsForValue().set(userId + ":accessToken", accessToken, Duration.ofHours(2));

        // refreshToken 저장 + 7일 만료
        redisTemplate.opsForValue().set(userId + ":refreshToken", refreshToken, Duration.ofDays(7));
    }

    // Redis에서 accessToken과 refreshToken을 조회
    public String getAccessTokenFromRedis(String userId) {
        return (String) redisTemplate.opsForValue().get(userId + ":accessToken");
    }

    public String getRefreshTokenFromRedis(String userId) {
        return (String) redisTemplate.opsForValue().get(userId + ":refreshToken");
    }

    public int saveTokenToDatabase(String type, int uid, String accessToken, String refreshToken) {
        if("social".equals(type)){
            Token findToken = tokenMapper.findTokenBySocialUid(uid);

            if (findToken != null) {
                return tokenMapper.updateSocialToken(
                        Token.builder()
                                .socialUid(uid)
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .build()
                );
            }else{
                return tokenMapper.saveSocialToken(
                        Token.builder()
                                .socialUid(uid)
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .build()
                );
            }
        }else{
            Token findToken = tokenMapper.findTokenByUserUid(uid);

            if (findToken != null) {
                return tokenMapper.updateUserToken(
                        Token.builder()
                                .userUid(uid)
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .build()
                );
            }else{
                return tokenMapper.saveUserToken(
                        Token.builder()
                                .userUid(uid)
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .build()
                );
            }
        }
    }

    @Transactional
    public boolean deleteTokenToRedis(String type, String userid){
        boolean resultAccessToken = redisTemplate.delete(type + ":" + userid + ":accessToken");
        boolean resultRefreshToken = redisTemplate.delete(type + ":" + userid + ":refreshToken");

        return resultAccessToken && resultRefreshToken;
    }

    public boolean deleteTokenToDatabase(String type, int uid){
        if("social".equals(type)){
            return tokenMapper.deleteTokenBySocialUid(uid)>0;
        }else{
            return tokenMapper.deleteTokenByUserUid(uid)>0;
        }
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
