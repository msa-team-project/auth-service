package com.example.authservice.service;

import com.example.authservice.config.redis.RedisUtil;
import com.example.authservice.config.security.CustomUserDetails;
import com.example.authservice.dto.*;
import com.example.authservice.mapper.AddressMapper;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.Address;
import com.example.authservice.model.Social;
import com.example.authservice.model.User;
import com.example.authservice.type.Role;
import com.example.authservice.type.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserMapper userMapper;
    private AddressMapper addressMapper;
    private AuthenticationManager authenticationManager;
    private TokenProviderService tokenProviderService;
    private EmailService emailService;
    private RedisUtil redisUtil;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        addressMapper = mock(AddressMapper.class);
        authenticationManager = mock(AuthenticationManager.class);
        tokenProviderService = mock(TokenProviderService.class);
        emailService = mock(EmailService.class);
        redisUtil = mock(RedisUtil.class);
        userService = new UserService(userMapper, addressMapper, authenticationManager, tokenProviderService, emailService, redisUtil);
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

        //verify(userMapper).activeSocial("user123");
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

    @Test
    void 회원가입_성공_테스트() {
        // given: 이메일 인증 플래그가 존재하고 true인 경우
        String email = "test@example.com";
        User user = User.builder().email(email).build();
        Address address = Address.builder()
                .mainAddress("Main Address")
                .mainLan(100.0)
                .mainLat(100.0)
                .build();
        when(redisUtil.existData(email + ":verified")).thenReturn(true);
        when(redisUtil.getData(email + ":verified")).thenReturn("true");
        when(userMapper.save(user)).thenReturn(user);
        doNothing().when(addressMapper).insertAddress(address);

        // when
        UserJoinResponseDTO result = userService.join(user, address);

        // then
        assertTrue(result.isSuccess());
        verify(redisUtil).deleteData(email + ":verified");
        verify(addressMapper).insertAddress(address);
    }

    @Test
    void 회원가입_실패_인증필요_테스트() {
        // given: 이메일 인증 플래그가 없거나 false인 경우
        String email = "fail@example.com";
        User user = User.builder().email(email).build();
        Address address = Address.builder()
                .mainAddress("Main Address")
                .mainLan(200.0)
                .mainLat(200.0)
                .build();
        when(redisUtil.existData(email + ":verified")).thenReturn(false);

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.join(user, address));
        assertEquals("이메일 인증이 필요합니다.", ex.getMessage());
    }

    @Test
    void 이메일_검증_통과_테스트() {
        // given
        String email = "user@mail.com";
        String code = "1234";
        when(emailService.verifyEmailCode(email, code)).thenReturn(true);

        // when
        boolean ok = userService.verifyEmail(email, code);

        // then
        assertTrue(ok);
    }

    @Test
    void 이메일_검증_실패_테스트() {
        // given
        String email = "user@mail.com";
        String code = "0000";
        when(emailService.verifyEmailCode(email, code)).thenReturn(false);

        // when
        boolean ok = userService.verifyEmail(email, code);

        // then
        assertFalse(ok);
    }

    @Test
    void 주소_수정_성공_테스트() {
        // given
        int uid = 42;
        UpdateAddressRequestDTO request = UpdateAddressRequestDTO.builder()
                .userUid(uid)
                .mainAddress("강남대로 123")
                .subAddress1("2층")
                .subAddress2("사무실 456호")
                .mainLat(37.4979)
                .mainLan(127.0276)
                .subLat1(37.4980)
                .subLan1(127.0277)
                .subLat2(37.4981)
                .subLan2(127.0278)
                .build();

        User user = User.builder().uid(uid).build();
        Address existing = Address.builder()
                .userUid(uid)
                .mainAddress("강남대로 OLD")
                .mainLat(1.0).mainLan(1.0)
                .sub1Lat(1.1).sub1Lan(1.1)
                .sub2Lat(1.2).sub2Lan(1.2)
                .build();

        when(userMapper.findUserByUserUid(uid)).thenReturn(user);
        when(addressMapper.findByUserUid(uid)).thenReturn(existing);
        doNothing().when(addressMapper).updateAddress(any(Address.class));

        // when
        UpdateAddressResponseDTO response = userService.updateAddress(uid, request);

        // then
        assertTrue(response.isSuccess());
        // 그리고 실제로 updateAddress 호출 시, 프로퍼티가 request 값으로 바뀌었는지 검증하고 싶다면
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).updateAddress(captor.capture());
        Address updated = captor.getValue();
        assertEquals("강남대로 123",   updated.getMainAddress());
        assertEquals("2층",            updated.getSubAddress1());
        assertEquals("사무실 456호",    updated.getSubAddress2());
        assertEquals(37.4979,          updated.getMainLat());
        assertEquals(127.0276,         updated.getMainLan());
        assertEquals(37.4980,          updated.getSub1Lat());
        assertEquals(127.0277,         updated.getSub1Lan());
        assertEquals(37.4981,          updated.getSub2Lat());
        assertEquals(127.0278,         updated.getSub2Lan());
    }

    @Test
    void 주소_수정_실패_사용자없음_테스트() {
        // given
        int uid = 99;
        when(userMapper.findUserByUserUid(uid)).thenReturn(null);

        // when & then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateAddress(uid,
                        UpdateAddressRequestDTO.builder().build()
                )
        );
        assertTrue(ex.getMessage().contains("존재하지 않는 사용자 UID"));
    }

    @Test
    void 주소_수정_실패_주소없음_테스트() {
        // given
        int uid = 50;
        User user = User.builder().uid(uid).build();
        when(userMapper.findUserByUserUid(uid)).thenReturn(user);
        when(addressMapper.findByUserUid(uid)).thenReturn(null);

        // when & then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateAddress(uid,
                        UpdateAddressRequestDTO.builder().build()
                )
        );
        assertTrue(ex.getMessage().contains("주소 정보가 없습니다"));
    }
}
