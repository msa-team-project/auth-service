package com.example.authservice.service;

import com.example.authservice.config.redis.RedisUtil;
import com.example.authservice.config.security.CustomUserDetails;
import com.example.authservice.dto.*;
import com.example.authservice.mapper.AddressMapper;
import com.example.authservice.mapper.TokenMapper;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.Address;
import com.example.authservice.model.Social;
import com.example.authservice.model.User;
import com.example.authservice.type.Role;
import com.example.authservice.type.Type;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static com.example.authservice.type.Role.ROLE_USER;
import static com.example.authservice.type.Type.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final AuthenticationManager authenticationManager;
    private final TokenProviderService tokenProviderService;
    private final EmailService emailService;
    private final RedisUtil redisUtil;

    @Transactional
    public UserLoginResponseDTO login(String username, String password) {
        System.out.println("login info is :: " + username + " " + password);

        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        SecurityContextHolder.getContext().setAuthentication(authenticate);

        User user = ((CustomUserDetails) authenticate.getPrincipal()).getUser();

        String accessToken = tokenProviderService.generateToken(user, Duration.ofHours(2));
        String refreshToken = tokenProviderService.generateToken(user, Duration.ofDays(2));

        System.out.println("accessToken is :: " + accessToken);
        System.out.println("refreshToken is :: " + refreshToken);

        // redis에 저장
        tokenProviderService.saveTokensToRedis("USER:"+user.getUserId(), accessToken, refreshToken);

        // DB에 저장
        tokenProviderService.saveTokenToDatabase("user",user.getUid(), accessToken, refreshToken);

        return UserLoginResponseDTO.builder()
                .loggedIn(true)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .userName(user.getUserName())
                .build();
    }

    @Transactional
    public UserJoinResponseDTO join(User user, Address address) {

        String email = user.getEmail();
        if (!redisUtil.existData(email + ":verified") || !"true".equals(redisUtil.getData(email + ":verified"))) {
            throw new RuntimeException("이메일 인증이 필요합니다.");
        }

        User result = userMapper.save(user);
        address.setUserUid(result.getUid());
        addressMapper.insertAddress(address);

        if(result == null){
            return UserJoinResponseDTO.builder()
                    .isSuccess(false)
                    .build();
        }else{
            // 가입 성공 후 인증 플래그 제거
            redisUtil.deleteData(email + ":verified");

            return UserJoinResponseDTO.builder()
                    .isSuccess(true)
                    .build();
        }
    }

    //이메일 인증 코드 검증 로직
    public boolean verifyEmail(String email, String code) {
        return emailService.verifyEmailCode(email, code);
    }

    public OAuthLoginResponseDTO oauthLogin(OAuthLoginRequestDTO oauthDTO){
        
        String[] tokens = oauthDTO.getAccessToken().split(":");

        Social findSocial;

        if("naver".equals(tokens[0])){
            findSocial = userMapper.findSocialByUserName(oauthDTO.getName());
        }else if("google".equals(tokens[0])){
            findSocial = userMapper.findSocialByUserName(oauthDTO.getName());
        }else if("kakao".equals(tokens[0])){
            // 지금은 정보요청 권한이 없어서 닉네임만 받아올수 있음
            findSocial = userMapper.findSocialByUserName(oauthDTO.getNickname());
        }else{
            return OAuthLoginResponseDTO.builder()
                    .loggedIn(false)
                    .message("ForbiddenToken")
                    .build();
        }

        if(findSocial == null){
            // 회원정보 저장 후 로그인 처리
            Social newSocial = buildNewSocialObj(tokens[0], oauthDTO);

            if(newSocial != null){
                int result = userMapper.saveSocial(newSocial);
                if(result == 1){
                    Social social = userMapper.findSocialByUserId(newSocial.getUserId());
                    // redis에 저장
                    tokenProviderService.saveTokensToRedis(tokens[0].toUpperCase()+":"+oauthDTO.getId(), oauthDTO.getAccessToken(), oauthDTO.getRefreshToken());

                    // DB에 저장
                    tokenProviderService.saveTokenToDatabase("social",social.getUid(),oauthDTO.getAccessToken(),oauthDTO.getRefreshToken());

                    return OAuthLoginResponseDTO.builder()
                            .loggedIn(true)
                            .type(newSocial.getType())
                            .userName(newSocial.getUserName())
                            .email(newSocial.getEmail())
                            .mobile(newSocial.getPhone())
                            .role(newSocial.getRole())
                            .accessToken(oauthDTO.getAccessToken())
                            .refreshToken(oauthDTO.getRefreshToken())
                            .build();
                }else{
                    return OAuthLoginResponseDTO.builder()
                            .loggedIn(false)
                            .build();
                }
            }else{
                return OAuthLoginResponseDTO.builder()
                        .loggedIn(false)
                        .message("Forbidden")
                        .build();
            }
        }else{
            // accessToken의 타입 잘라서 타입이 일치하면 로그인 처리
            // 다른 타입이면 이미 가입한 계정이 있다고 응답
            System.out.println("find type is :: " + findSocial.getType().name());
            System.out.println("tokens type is :: " + tokens[0]);
            if(findSocial.getType().name().toLowerCase().equals(tokens[0])){

                // redis에 저장
                tokenProviderService.saveTokensToRedis(findSocial.getType().name()+":"+oauthDTO.getId(), oauthDTO.getAccessToken(), oauthDTO.getRefreshToken());

                // DB에 저장
                tokenProviderService.saveTokenToDatabase("social",findSocial.getUid(),oauthDTO.getAccessToken(),oauthDTO.getRefreshToken());
                
                return OAuthLoginResponseDTO.builder()
                            .loggedIn(true)
                            .type(findSocial.getType())
                            .userName(findSocial.getUserName())
                            .email(findSocial.getEmail())
                            .mobile(findSocial.getPhone())
                            .role(findSocial.getRole())
                            .accessToken(oauthDTO.getAccessToken())
                            .refreshToken(oauthDTO.getRefreshToken())
                            .build();
            }else{
                return OAuthLoginResponseDTO.builder()
                        .loggedIn(false)
                        .type(findSocial.getType())
                        .message("AlreadyExists")
                        .build();
            }
        }
    }

    protected Social buildNewSocialObj (String type, OAuthLoginRequestDTO oauthDTO) {
        if("naver".equals(type)){
            return Social.builder()
                    .userId(oauthDTO.getId())
                    .userName(oauthDTO.getName())
                    .email("")
                    .emailyn("n")
                    .phone(oauthDTO.getMobile())
                    .phoneyn("n")
                    .type(NAVER)
                    .role(ROLE_USER)
                    .build();
        }else if("google".equals(type)){
            return Social.builder()
                    .userId(oauthDTO.getId())
                    .userName(oauthDTO.getName())
                    .email(oauthDTO.getEmail())
                    .emailyn("n")
                    .phone("")
                    .phoneyn("n")
                    .type(GOOGLE)
                    .role(ROLE_USER)
                    .build();
        }else if("kakao".equals(type)){
            // 지금은 정보요청 권한이 없어서 닉네임만 받아올수 있음
            return Social.builder()
                    .userId(oauthDTO.getId())
                    .userName(oauthDTO.getNickname())
                    .email("")
                    .emailyn("n")
                    .phone("")
                    .phoneyn("n")
                    .type(KAKAO)
                    .role(ROLE_USER)
                    .build();
        }else{
            return null;
        }
    }

    public LogoutResponseDTO logout(String token) {
        String[] splitArr = token.split(":");
        boolean redisResult;
        boolean dbResult;

        if("naver".equals(splitArr[0]) || "kakao".equals(splitArr[0]) || "google".equals(splitArr[0])){
            redisResult = tokenProviderService.deleteTokenToRedis(splitArr[0].toUpperCase(),splitArr[1]);
            System.out.println("userId is :: " + splitArr[1]);
            Social findSocial = userMapper.findSocialByUserId(splitArr[1]);
            dbResult = tokenProviderService.deleteTokenToDatabase("social",findSocial.getUid());
        }else{
            String resultUserId = tokenProviderService.getTokenDetails(token).getUserId();
            redisResult = tokenProviderService.deleteTokenToRedis("USER",resultUserId);
            User user = userMapper.findUserByUserId(resultUserId);
            dbResult = tokenProviderService.deleteTokenToDatabase("user",user.getUid());
        }
        return redisResult&&dbResult?
                LogoutResponseDTO.builder()
                        .successed(true)
                        .build() :
                LogoutResponseDTO.builder()
                        .successed(false)
                        .build();
    }

    public UserInfoResponseDTO getUserInfo(String token) {
        String[] splitArr = token.split(":");

        if("naver".equals(splitArr[0]) || "kakao".equals(splitArr[0]) || "google".equals(splitArr[0])){
            Social findSocial = userMapper.findSocialByUserId(splitArr[1]);

            return UserInfoResponseDTO.builder()
                    .id(Long.valueOf(findSocial.getUid()))
                    .userId(findSocial.getUserId())
                    .userName(findSocial.getUserName())
                    .role(findSocial.getRole())
                    .build();
        }else{
            User findUser = tokenProviderService.getTokenDetails(token);

            return UserInfoResponseDTO.builder()
                    .id(Long.valueOf(findUser.getUid()))
                    .userId(findUser.getUserId())
                    .userName(findUser.getUserName())
                    .role(findUser.getRole())
                    .build();
        }
    }

    @Transactional
    public LogoutResponseDTO deleteAccount(String token) {
        LogoutResponseDTO removeTokenResult = logout(token);

        String[] splitArr = token.split(":");

        int result;

        if("naver".equals(splitArr[0]) || "kakao".equals(splitArr[0]) || "google".equals(splitArr[0])){
            result = userMapper.deleteSocial(splitArr[1]);
        }else{
            User findUser = tokenProviderService.getTokenDetails(token);
            result = userMapper.deleteUser(findUser.getUserId());
        }
        return LogoutResponseDTO.builder()
                .successed((result>0)&& removeTokenResult.isSuccessed())
                .build();
    }

//    //주소 변경 (수정 중)
//    @Transactional
//    public User updateAddress(int uid, UpdateAddressRequestDTO request) {
//        // 1) DB에서 유저 조회
//        User user = userMapper.findUserByUserUid(uid);
//        // 2) 주소 변경 (수정 중)
//        // 3) 저장
//        User saved = userMapper.save(user);
//
//        // 4) 변경된 후 사용자 정보 반환
//        return User.builder()
//                .userId(saved.getUserId())
//                .userName(saved.getUserName())
//                .email(saved.getEmail())
//                .emailyn(saved.getEmailyn())
//                .phone(saved.getPhone())
//                .phoneyn(saved.getPhoneyn())
//                .address(saved.getAddress())
//                .point(saved.getPoint())
//                .role(saved.getRole())
//                .build();
//    }
}
