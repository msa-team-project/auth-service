package com.example.authservice.service;

import com.example.authservice.config.redis.RedisUtil;
import com.example.authservice.config.security.CustomUserDetails;
import com.example.authservice.dto.*;
import com.example.authservice.mapper.AddressMapper;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.Address;
import com.example.authservice.model.Social;
import com.example.authservice.model.User;
import lombok.RequiredArgsConstructor;
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

        // 1) save 후 null 체크를 최우선으로
        User result = userMapper.save(user);
        if (result == null) {
            // save 가 실패했다면 NPE 대신 곧바로 실패 DTO 리턴
            return UserJoinResponseDTO.builder()
                    .isSuccess(false)
                    .build();
        }

        // 2) result 가 null 이 아니면 정상적으로 address 처리
        address.setUserUid(result.getUid());
        addressMapper.insertAddress(address);

        // 3) 인증 플래그 제거 및 성공 DTO 반환
        redisUtil.deleteData(email + ":verified");
        return UserJoinResponseDTO.builder()
                .isSuccess(true)
                .build();
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

    //주소 변경
    @Transactional
    public UpdateAddressResponseDTO updateAddress(int uid, UpdateAddressRequestDTO request) {
        // 1) 유저 존재 확인 (선택)
        User user = userMapper.findUserByUserUid(uid);
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자 UID: " + uid);
        }

        // 2) 기존 Address 조회
        Address address = addressMapper.findByUserUid(user.getUid());
        if (address == null) {
            throw new IllegalArgumentException("주소 정보가 없습니다. userUid=" + uid);
        }

        // 3) Address 엔티티에 변경 사항 반영
        address.setMainAddress(request.getMainAddress());
        address.setSubAddress1(request.getSubAddress1());
        address.setSubAddress2(request.getSubAddress2());
        address.setMainLat(request.getMainLat());
        address.setMainLan(request.getMainLan());
        address.setSub1Lat(request.getSubLat1());
        address.setSub1Lan(request.getSubLan1());
        address.setSub2Lat(request.getSubLat2());
        address.setSub2Lan(request.getSubLan2());

        // 4) DB 업데이트
        addressMapper.updateAddress(address);

        // 5) 결과 DTO 리턴
        return UpdateAddressResponseDTO.builder()
                .isSuccess(true)
                .build();
    }
}
