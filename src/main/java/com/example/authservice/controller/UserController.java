package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/user/info")
    public UserInfoResponseDTO getUserInfo(HttpServletRequest request){
        String token = request.getHeader("Authorization").substring(7);
        log.info("user info :: {}", token);
        return userService.getUserInfo(token);
    }

    @PostMapping("/profile")
    public ProfileResponseDTO getUserProfile(HttpServletRequest request){
        String token = request.getHeader("Authorization").substring(7);
        log.info("user profile :: {}", token);
        return userService.getUserProfile(token);
    }

    @PostMapping("/logout")
    public LogoutResponseDTO logout(HttpServletRequest request){
        String token = request.getHeader("Authorization").substring(7);
        log.info("logout :: {}",token);
        return userService.logout(token);
    }

    @DeleteMapping("/user")
    public LogoutResponseDTO deleteAccount(HttpServletRequest request){
        String token = request.getHeader("Authorization").substring(7);
        log.info("delete account :: {}", token);
        return userService.deleteAccount(token);
    }

    //주소 수정
    @PutMapping("/{uid}/address")
    public void updateUserAddress(@PathVariable int uid, @RequestBody UpdateAddressRequestDTO request) {
        log.info("update user address :: {}", request);
    }
}
