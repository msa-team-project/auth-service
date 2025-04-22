package com.example.authservice.mapper;

import com.example.authservice.model.Social;
import com.example.authservice.model.User;
import feign.Param;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    // 실제 데이터 삽입을 수행하는 메서드 (반환타입은 void나 int)
    void insertUser(User user);

    // 기존의 save 메서드를 default 메서드로 구현하여 파라미터 객체를 반환
    default User save(User user) {
        insertUser(user); // insertUser를 호출하면 useGeneratedKeys 옵션에 의해 user.uid가 자동 채워짐
        return user;
    }

    int countByEmail(@Param("email") String email);
    User findUserByUserId(String userId);
    User findUserByUserUid(int uid);
    Social findSocialByUserName(String userName);
    Social findSocialByUserId(String userId);
    int saveSocial(Social social);
    int deleteUser(String userId);
    int deleteSocial(String userId);
    int activeSocial(String userId);
    int updateSocial(Social social);
    int updateUser(User user);
}
