package com.example.authservice.service;

import com.example.authservice.config.security.CustomUserDetails;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailService implements UserDetailsService {
    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("찾기전 :: " + username);
        User userByUserId = userMapper.findUserByUserId(username);
        System.out.println("찾기후 :: " + userByUserId.getUserName());
        if(userByUserId == null){
            throw new UsernameNotFoundException(username);
        }

        return CustomUserDetails.builder()
                .user(userByUserId)
                .roles(List.of(String.valueOf(userByUserId.getRole())))
                .build();
    }
}
