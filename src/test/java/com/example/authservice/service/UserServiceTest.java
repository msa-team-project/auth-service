package com.example.authservice.service;

import com.example.authservice.config.redis.RedisUtil;
import com.example.authservice.config.security.CustomUserDetails;
import com.example.authservice.dto.*;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.Social;
import com.example.authservice.model.User;
import com.example.authservice.type.Role;
import com.example.authservice.type.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;


import static com.example.authservice.type.Type.GOOGLE;
import static com.example.authservice.type.Type.NAVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserMapper userMapper;
    private AuthenticationManager authenticationManager;
    private TokenProviderService tokenProviderService;
    private EmailService emailService;
    private RedisUtil redisUtil;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        authenticationManager = mock(AuthenticationManager.class);
        tokenProviderService = mock(TokenProviderService.class);
        emailService = mock(EmailService.class);
        redisUtil = mock(RedisUtil.class);
        userService = new UserService(userMapper, authenticationManager, tokenProviderService, emailService, redisUtil);
    }

    @Test
    void login_success() {
        String username = "testuser";
        String password = "password";

        User user = User.builder()
                .uid(1)
                .userId("user123")
                .userName("Test User")
                .role(Role.ROLE_USER)
                .build();

        CustomUserDetails customUserDetails = new CustomUserDetails(user, List.of("ROLE_USER"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(customUserDetails, null);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        // 만약 generateToken이 두 개의 값을 리턴한다면, DTO나 Map을 사용한다고 가정하고 예시로 작성
        when(tokenProviderService.generateToken(eq(user), any()))
                .thenReturn("access-token")  // 이 부분은 메서드 정의를 정확히 알면 더 정확히 수정 가능
                .thenReturn("refresh-token");

        UserLoginResponseDTO response = userService.login(username, password);

        assertThat(response.isLoggedIn()).isTrue();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        verify(tokenProviderService).saveTokensToRedis(anyString(), anyString(), anyString());
        verify(tokenProviderService).saveTokenToDatabase(anyString(), anyInt(), anyString(), any());
    }

    @Test
    void oauthLogin_new_social_user_should_register_and_login() {
        OAuthLoginRequestDTO dto = OAuthLoginRequestDTO.builder()
                .accessToken("google:user123")
                .refreshToken("refresh-token")
                .id("user123")
                .name("John Doe")
                .email("john@example.com")
                .build();

        when(userMapper.findSocialByUserName(dto.getName())).thenReturn(null);
        when(userMapper.saveSocial(any())).thenReturn(1);

        Social saved = Social.builder()
                .uid(100)
                .userId("user123")
                .userName("John Doe")
                .type(GOOGLE)
                .role(Role.ROLE_USER)
                .build();

        when(userMapper.findSocialByUserId("user123")).thenReturn(saved);

        OAuthLoginResponseDTO response = userService.oauthLogin(dto);

        assertThat(response.isLoggedIn()).isTrue();
        assertThat(response.getType()).isEqualTo(GOOGLE);
    }

    @Test
    void logout_user_account() {
        String accessToken = "USER:user123";

        User user = User.builder()
                .uid(1)
                .userId("user123")
                .build();

        when(tokenProviderService.getTokenDetails(accessToken)).thenReturn(user);
        when(tokenProviderService.deleteTokenToRedis("USER", "user123")).thenReturn(true);
        when(tokenProviderService.deleteTokenToDatabase("user", 1)).thenReturn(true);
        when(userMapper.findUserByUserId("user123")).thenReturn(user);

        LogoutResponseDTO response = userService.logout(accessToken);

        assertThat(response.isSuccessed()).isTrue();
    }

    @Test
    void getUserInfo_for_user_account() {
        String token = "USER:user123";

        User user = User.builder()
                .uid(1)
                .userId("user123")
                .userName("TestUser")
                .role(Role.ROLE_USER)
                .build();

        when(tokenProviderService.getTokenDetails(token)).thenReturn(user);

        UserInfoResponseDTO dto = userService.getUserInfo(token);

        assertThat(dto.getUserId()).isEqualTo("user123");
        assertThat(dto.getUserName()).isEqualTo("TestUser");
    }

    @Test
    void deleteAccount_user() {
        String token = "USER:user123";

        User user = User.builder()
                .uid(1)
                .userId("user123")
                .build();

        when(tokenProviderService.getTokenDetails(token)).thenReturn(user);
        when(tokenProviderService.deleteTokenToRedis("USER", "user123")).thenReturn(true);
        when(tokenProviderService.deleteTokenToDatabase("user", 1)).thenReturn(true);
        when(userMapper.findUserByUserId("user123")).thenReturn(user);
        when(userMapper.deleteUser("user123")).thenReturn(1);

        LogoutResponseDTO response = userService.deleteAccount(token);

        assertThat(response.isSuccessed()).isTrue();
    }

    @Test
    void login_should_fail_when_authentication_fails() {
        String username = "testuser";
        String password = "wrong";

        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("Invalid credentials"));

        assertThrows(RuntimeException.class, () -> userService.login(username, password));
    }

    @Test
    void oauthLogin_with_unknown_provider_should_return_forbidden() {
        OAuthLoginRequestDTO dto = OAuthLoginRequestDTO.builder()
                .accessToken("unknown:user123")
                .build();

        OAuthLoginResponseDTO response = userService.oauthLogin(dto);

        assertThat(response.isLoggedIn()).isFalse();
        assertThat(response.getMessage()).isEqualTo("ForbiddenToken");
    }

    @Test
    void oauthLogin_existing_social_account_with_different_type_should_fail() {
        OAuthLoginRequestDTO dto = OAuthLoginRequestDTO.builder()
                .accessToken("google:user123")
                .id("user123")
                .name("John Doe")
                .email("john@example.com")
                .build();

        Social existing = Social.builder()
                .userId("user123")
                .userName("John Doe")
                .type(NAVER)  // 다른 타입
                .build();

        when(userMapper.findSocialByUserName("John Doe")).thenReturn(existing);

        OAuthLoginResponseDTO response = userService.oauthLogin(dto);

        assertThat(response.isLoggedIn()).isFalse();
        assertThat(response.getMessage()).isEqualTo("AlreadyExists");
    }

    @Test
    void oauthLogin_existing_deleted_social_should_reactivate_and_login() {
        OAuthLoginRequestDTO dto = OAuthLoginRequestDTO.builder()
                .accessToken("google:user123")
                .refreshToken("ref-token")
                .id("user123")
                .name("John Doe")
                .build();

        Social existing = Social.builder()
                .userId("user123")
                .userName("John Doe")
                .type(GOOGLE)
                .status("deleted")
                .build();

        when(userMapper.findSocialByUserName("John Doe")).thenReturn(existing);

        OAuthLoginResponseDTO response = userService.oauthLogin(dto);

        verify(userMapper).activeSocial("user123");
        assertThat(response.isLoggedIn()).isTrue();
    }

    @Test
    void logout_should_fail_if_redis_deletion_fails() {
        String token = "USER:user123";
        User user = User.builder().uid(1).userId("user123").build();

        when(tokenProviderService.getTokenDetails(token)).thenReturn(user);
        when(tokenProviderService.deleteTokenToRedis("USER", "user123")).thenReturn(false);
        when(tokenProviderService.deleteTokenToDatabase("user", 1)).thenReturn(true);
        when(userMapper.findUserByUserId("user123")).thenReturn(user);

        LogoutResponseDTO response = userService.logout(token);

        assertThat(response.isSuccessed()).isFalse();
    }

    @Test
    void getUserInfo_for_social_account() {
        String token = "google:user123";

        Social social = Social.builder()
                .uid(10)
                .userId("user123")
                .userName("John")
                .role(Role.ROLE_USER)
                .build();

        when(userMapper.findSocialByUserId("user123")).thenReturn(social);

        UserInfoResponseDTO response = userService.getUserInfo(token);

        assertThat(response.getUserId()).isEqualTo("user123");
        assertThat(response.getUserName()).isEqualTo("John");
        assertThat(response.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void deleteAccount_should_fail_if_db_deletion_fails() {
        String token = "USER:user123";
        User user = User.builder().uid(1).userId("user123").build();

        when(tokenProviderService.getTokenDetails(token)).thenReturn(user);
        when(tokenProviderService.deleteTokenToRedis("USER", "user123")).thenReturn(true);
        when(tokenProviderService.deleteTokenToDatabase("user", 1)).thenReturn(true);
        when(userMapper.findUserByUserId("user123")).thenReturn(user);
        when(userMapper.deleteUser("user123")).thenReturn(0); // 삭제 실패

        LogoutResponseDTO response = userService.deleteAccount(token);

        assertThat(response.isSuccessed()).isFalse();
    }
    @Test
    void buildNewSocialObj_should_return_social_for_naver() {
        OAuthLoginRequestDTO oauthDTO = OAuthLoginRequestDTO.builder()
                .id("user123")
                .name("John Doe")
                .mobile("01012345678")
                .build();

        Social social = userService.buildNewSocialObj("naver", oauthDTO);

        assertThat(social).isNotNull();
        assertThat(social.getType()).isEqualTo(Type.NAVER);
        assertThat(social.getUserId()).isEqualTo("user123");
        assertThat(social.getUserName()).isEqualTo("John Doe");
        assertThat(social.getPhone()).isEqualTo("01012345678");
    }

    @Test
    void buildNewSocialObj_should_return_social_for_google() {
        OAuthLoginRequestDTO oauthDTO = OAuthLoginRequestDTO.builder()
                .id("user123")
                .name("John Doe")
                .email("john.doe@example.com")
                .build();

        Social social = userService.buildNewSocialObj("google", oauthDTO);

        assertThat(social).isNotNull();
        assertThat(social.getType()).isEqualTo(Type.GOOGLE);
        assertThat(social.getUserId()).isEqualTo("user123");
        assertThat(social.getUserName()).isEqualTo("John Doe");
        assertThat(social.getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void buildNewSocialObj_should_return_social_for_kakao() {
        OAuthLoginRequestDTO oauthDTO = OAuthLoginRequestDTO.builder()
                .id("user123")
                .nickname("John Doe")
                .build();

        Social social = userService.buildNewSocialObj("kakao", oauthDTO);

        assertThat(social).isNotNull();
        assertThat(social.getType()).isEqualTo(Type.KAKAO);
        assertThat(social.getUserId()).isEqualTo("user123");
        assertThat(social.getUserName()).isEqualTo("John Doe");
    }

    @Test
    void buildNewSocialObj_should_return_null_for_unknown_type() {
        OAuthLoginRequestDTO oauthDTO = OAuthLoginRequestDTO.builder()
                .id("user123")
                .name("Unknown User")
                .build();

        Social social = userService.buildNewSocialObj("unknown", oauthDTO);

        assertThat(social).isNull();
    }
}
