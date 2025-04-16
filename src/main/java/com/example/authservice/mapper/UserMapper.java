package com.example.authservice.mapper;

import com.example.authservice.model.Social;
import com.example.authservice.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    User save(User user);
    User findUserByUserId(String userId);
    Social findSocialByUserName(String userName);
    Social findSocialByUserId(String userId);
    int saveSocial(Social social);
    int deleteUser(String userId);
    int deleteSocial(String userId);
}
