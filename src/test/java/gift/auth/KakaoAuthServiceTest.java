package gift.auth;

import gift.TestFixtures;
import gift.member.Member;
import gift.member.MemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KakaoAuthServiceTest {

    @Mock
    private KakaoLoginProperties properties;

    @Mock
    private KakaoLoginClient kakaoLoginClient;

    @Mock
    private MemberService memberService;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private KakaoAuthService kakaoAuthService;

    @Test
    void loginOrRegister_existingMember_updatesTokenReturnsJwt() {
        var member = TestFixtures.member(1L, "user@kakao.com", null);
        given(kakaoLoginClient.requestAccessToken("code123"))
            .willReturn(new KakaoLoginClient.KakaoTokenResponse("kakao-token"));
        given(kakaoLoginClient.requestUserInfo("kakao-token"))
            .willReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("user@kakao.com")));
        given(memberService.findOrCreateByEmail("user@kakao.com")).willReturn(member);
        given(jwtProvider.createToken("user@kakao.com")).willReturn("jwt-token");

        var result = kakaoAuthService.loginOrRegister("code123");

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(member.getKakaoAccessToken()).isEqualTo("kakao-token");
    }

    @Test
    void loginOrRegister_newMember_createsAndReturnsJwt() {
        var newMember = new Member("new@kakao.com");
        given(kakaoLoginClient.requestAccessToken("code123"))
            .willReturn(new KakaoLoginClient.KakaoTokenResponse("kakao-token"));
        given(kakaoLoginClient.requestUserInfo("kakao-token"))
            .willReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("new@kakao.com")));
        given(memberService.findOrCreateByEmail("new@kakao.com")).willReturn(newMember);
        given(jwtProvider.createToken("new@kakao.com")).willReturn("jwt-token");

        var result = kakaoAuthService.loginOrRegister("code123");

        assertThat(result.token()).isEqualTo("jwt-token");
        verify(memberService).findOrCreateByEmail("new@kakao.com");
    }

    @Test
    void loginOrRegister_savesKakaoAccessToken() {
        var member = TestFixtures.member(1L, "user@kakao.com", null);
        given(kakaoLoginClient.requestAccessToken("code123"))
            .willReturn(new KakaoLoginClient.KakaoTokenResponse("new-kakao-token"));
        given(kakaoLoginClient.requestUserInfo("new-kakao-token"))
            .willReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("user@kakao.com")));
        given(memberService.findOrCreateByEmail("user@kakao.com")).willReturn(member);
        given(jwtProvider.createToken("user@kakao.com")).willReturn("jwt");

        kakaoAuthService.loginOrRegister("code123");

        assertThat(member.getKakaoAccessToken()).isEqualTo("new-kakao-token");
    }

    @Test
    void loginOrRegister_generatesJwtWithEmail() {
        var newMember = new Member("test@kakao.com");
        given(kakaoLoginClient.requestAccessToken("code123"))
            .willReturn(new KakaoLoginClient.KakaoTokenResponse("kakao-token"));
        given(kakaoLoginClient.requestUserInfo("kakao-token"))
            .willReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("test@kakao.com")));
        given(memberService.findOrCreateByEmail("test@kakao.com")).willReturn(newMember);
        given(jwtProvider.createToken("test@kakao.com")).willReturn("jwt");

        kakaoAuthService.loginOrRegister("code123");

        verify(jwtProvider).createToken("test@kakao.com");
    }

    @Test
    void buildAuthorizationUrl_returnsValidKakaoUrl() {
        given(properties.clientId()).willReturn("test-client-id");
        given(properties.redirectUri()).willReturn("http://localhost:8080/api/auth/kakao/callback");

        var url = kakaoAuthService.buildAuthorizationUrl();

        assertThat(url).contains("https://kauth.kakao.com/oauth/authorize");
        assertThat(url).contains("client_id=test-client-id");
        assertThat(url).contains("redirect_uri=http://localhost:8080/api/auth/kakao/callback");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("scope=account_email,talk_message");
    }

    @Test
    void loginOrRegister_kakaoClientError_propagates() {
        given(kakaoLoginClient.requestAccessToken("bad-code"))
            .willThrow(new RuntimeException("카카오 API 오류"));

        assertThatThrownBy(() -> kakaoAuthService.loginOrRegister("bad-code"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("카카오 API 오류");
    }
}
