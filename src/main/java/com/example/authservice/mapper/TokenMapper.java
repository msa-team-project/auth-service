package com.example.authservice.mapper;

import com.example.authservice.model.Token;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TokenMapper {
    int saveUserToken(Token token);
    int saveSocialToken(Token token);
    Token findTokenByAccessToken(String accessToken);
    Token findTokenByRefreshToken(String refreshToken);
    int updateUserToken(Token token);
    int updateSocialToken(Token token);
    void deleteToken();
}
