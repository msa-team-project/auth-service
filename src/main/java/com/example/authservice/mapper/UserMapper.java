package com.example.authservice.mapper;

import com.example.authservice.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    int save(User user);
    User findUserByUserId(String userId);
}
