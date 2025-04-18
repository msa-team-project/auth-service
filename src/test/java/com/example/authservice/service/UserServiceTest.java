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
    private AddressMapper addressMapper;

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private EmailService emailService;

    @Mock
    private TokenProviderService tokenProviderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userMapper, addressMapper, authenticationManager, tokenProviderService, emailService, redisUtil);
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
    void 회원가입_실패_저장_실패_테스트() {
        // given
        String email = "foo@bar.com";
        User user = User.builder().email(email).build();
        Address address = Address.builder()
                .mainAddress("메인주소")
                .mainLat(1.1).mainLan(2.2)
                .build();

        // 인증 플래그는 통과했다고 치고
        when(redisUtil.existData(email + ":verified")).thenReturn(true);
        when(redisUtil.getData(email + ":verified")).thenReturn("true");
        // save가 null을 리턴하게 해서 실패 분기 타기
        when(userMapper.save(user)).thenReturn(null);

        // when
        UserJoinResponseDTO resp = userService.join(user, address);

        // then
        assertFalse(resp.isSuccess(), "userMapper.save가 null이면 isSuccess=false여야 한다");
        // 인증 플래그는 제거하지 않아야 함
        verify(redisUtil, never()).deleteData(anyString());
        // addressMapper도 호출되지 않아야 함
        verify(addressMapper, never()).insertAddress(any());
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
