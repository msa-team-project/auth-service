package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auths")
public class UserController {

    private final UserService userService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @PostMapping("/login")
    public UserLoginResponseDTO login(@RequestBody UserLoginRequestDTO userLoginRequestDTO) {
        log.info("login");
        return userService.login(userLoginRequestDTO.getUserId(), userLoginRequestDTO.getPassword());
    }

    @PostMapping("/join")
    public UserJoinResponseDTO join(@RequestBody UserJoinRequestDTO userJoinRequestDTO) {
        log.info("join :: {}", userJoinRequestDTO.getUserName() + " " + userJoinRequestDTO.getEmail());
        return userService.join(userJoinRequestDTO.toUser(bCryptPasswordEncoder));
    }

    @PostMapping("/login/oauth")
    public OAuthLoginResponseDTO socialLogin(@RequestBody OAuthLoginRequestDTO oauthLoginRequestDTO) {
        log.info("oauth login :: {}", oauthLoginRequestDTO.getAccessToken());
        return userService.oauthLogin(oauthLoginRequestDTO);
    }
}
