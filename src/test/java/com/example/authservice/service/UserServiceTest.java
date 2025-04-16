package com.example.authservice.service;

import com.example.authservice.config.security.CustomUserDetails;
import com.example.authservice.dto.OAuthLoginRequestDTO;
import com.example.authservice.dto.OAuthLoginResponseDTO;
import com.example.authservice.dto.UserLoginResponseDTO;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.Social;
import com.example.authservice.model.User;
import com.example.authservice.type.Role;
import com.example.authservice.type.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest  {

    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserMapper userMapper;

    @Mock
    private TokenProviderService tokenProviderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userMapper, authenticationManager, tokenProviderService);
    }

    @Test
    void login_should_return_tokens_and_userInfo() {
        // given
        String username = "testuser";
        String password = "testpass";

        User user = User.builder()
                .uid(1)
                .userId("testuser123")
                .userName("테스터")
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user, List.of("ROLE_USER"));
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, List.of());


        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenProviderService.generateToken(eq(user), eq(Duration.ofHours(2))))
                .thenReturn("access-token-abc");
        when(tokenProviderService.generateToken(eq(user), eq(Duration.ofDays(2))))
                .thenReturn("refresh-token-xyz");

        // when
        UserLoginResponseDTO response = userService.login(username, password);

        // then
        assertTrue(response.isLoggedIn());
        assertEquals("access-token-abc", response.getAccessToken());
        assertEquals("refresh-token-xyz", response.getRefreshToken());
        assertEquals("testuser123", response.getUserId());
        assertEquals("테스터", response.getUserName());

        // verify side-effects
        verify(tokenProviderService).saveTokensToRedis("USER:testuser123", "access-token-abc", "refresh-token-xyz");
        verify(tokenProviderService).updateTokenToDatabase("user", 1, "access-token-abc", "refresh-token-xyz");
    }

    @Test
    void shouldLoginSuccessfullyWhenSocialExistsAndTypeMatches() {
        OAuthLoginRequestDTO dto = OAuthLoginRequestDTO.builder()
                .accessToken("google:token123")
                .refreshToken("refresh123")
                .id("socialId")
                .name("tester")
                .nickname("테스터")
                .build();

        Social social = Social.builder()
                .uid(1)
                .userName("tester")
                .email("test@gmail.com")
                .phone("01012345678")
                .role(Role.ROLE_USER)
                .type(Type.GOOGLE)
                .build();

        when(userMapper.findSocialByUserName("tester")).thenReturn(social);

        OAuthLoginResponseDTO response = userService.oauthLogin(dto);

        assertTrue(response.isLoggedIn());
        assertEquals(Type.GOOGLE, response.getType());
        verify(tokenProviderService).saveTokensToRedis("GOOGLE:socialId", "google:token123", "refresh123");
        verify(tokenProviderService).updateTokenToDatabase("social", 1, "google:token123", "refresh123");
    }

    @Test
    void shouldCreateNewSocialWhenNotExists() {
        OAuthLoginRequestDTO dto = OAuthLoginRequestDTO.builder()
                .accessToken("naver:abc123")
                .refreshToken("refresh123")
                .id("socialId")
                .name("newuser")
                .mobile("01012341234")
                .build();

        when(userMapper.findSocialByUserName("newuser")).thenReturn(null);
        when(userMapper.saveSocial(any())).thenReturn(1);
        when(userMapper.findSocialByUserName("newuser")).thenReturn(
                Social.builder()
                        .uid(2)
                        .userName("newuser")
                        .email("")
                        .phone("01012341234")
                        .role(Role.ROLE_USER)
                        .type(Type.NAVER)
                        .build()
        );

        OAuthLoginResponseDTO response = userService.oauthLogin(dto);

        assertTrue(response.isLoggedIn());
        assertEquals(Type.NAVER, response.getType());
    }

    @Test
    void shouldReturnForbiddenTokenWhenUnknownProvider() {
        OAuthLoginRequestDTO dto = OAuthLoginRequestDTO.builder()
                .accessToken("facebook:abc")
                .refreshToken("refresh123")
                .id("id")
                .name("name")
                .build();

        OAuthLoginResponseDTO response = userService.oauthLogin(dto);

        assertFalse(response.isLoggedIn());
        assertEquals("ForbiddenToken", response.getMessage());
    }

    @Test
    void shouldReturnAlreadyExistsWhenTypeMismatch() {
        OAuthLoginRequestDTO dto = OAuthLoginRequestDTO.builder()
                .accessToken("naver:abc123")
                .refreshToken("refresh123")
                .id("socialId")
                .name("existinguser")
                .build();

        Social social = Social.builder()
                .uid(3)
                .userName("existinguser")
                .type(Type.KAKAO)
                .build();

        when(userMapper.findSocialByUserName("existinguser")).thenReturn(social);

        OAuthLoginResponseDTO response = userService.oauthLogin(dto);

        assertFalse(response.isLoggedIn());
        assertEquals("AlreadyExists", response.getMessage());
        assertEquals(Type.KAKAO, response.getType());
    }

    @Test
    void shouldReturnForbiddenWhenNewSocialIsNull() {
        OAuthLoginRequestDTO dto = OAuthLoginRequestDTO.builder()
                .accessToken("unknown:token")
                .refreshToken("refresh")
                .id("id")
                .name("name")
                .build();

        OAuthLoginResponseDTO response = userService.oauthLogin(dto);

        assertFalse(response.isLoggedIn());
        assertEquals("ForbiddenToken", response.getMessage());
    }

    @Test
    void shouldCreateSocialForNaver() {
        OAuthLoginRequestDTO dto = OAuthLoginRequestDTO.builder()
                .name("naverUser")
                .mobile("01012345678")
                .build();

        Social social = userService.buildNewSocialObj("naver", dto);

        assertNotNull(social);
        assertEquals("naverUser", social.getUserName());
        assertEquals(Type.NAVER, social.getType());
        assertEquals(Role.ROLE_USER, social.getRole());
        assertEquals("01012345678", social.getPhone());
    }

    @Test
    void shouldCreateSocialForGoogle() {
        OAuthLoginRequestDTO dto = OAuthLoginRequestDTO.builder()
                .name("googleUser")
                .email("google@user.com")
                .build();

        Social social = userService.buildNewSocialObj("google", dto);

        assertNotNull(social);
        assertEquals("googleUser", social.getUserName());
        assertEquals(Type.GOOGLE, social.getType());
        assertEquals(Role.ROLE_USER, social.getRole());
        assertEquals("google@user.com", social.getEmail());
    }

    @Test
    void shouldCreateSocialForKakao() {
        OAuthLoginRequestDTO dto = OAuthLoginRequestDTO.builder()
                .nickname("kakaoUser")
                .build();

        Social social = userService.buildNewSocialObj("kakao", dto);

        assertNotNull(social);
        assertEquals("kakaoUser", social.getUserName());
        assertEquals(Type.KAKAO, social.getType());
        assertEquals(Role.ROLE_USER, social.getRole());
    }

    @Test
    void shouldReturnNullForUnknownType() {
        OAuthLoginRequestDTO dto = OAuthLoginRequestDTO.builder()
                .name("unknownUser")
                .build();

        Social social = userService.buildNewSocialObj("unknown", dto);

        assertNull(social);
    }
}
