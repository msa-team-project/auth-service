package com.example.authservice.mapper;

import com.example.authservice.model.Token;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TokenMapper {
    int saveUserToken(Token token);
    int saveSocialToken(Token token);
    Token findTokenByUserUid(int userUid);
    Token findTokenBySocialUid(int socialUid);
    int updateUserToken(Token token);
    int updateSocialToken(Token token);
    int deleteTokenByUserUid(int userUid);
    int deleteTokenBySocialUid(int socialUid);
}
