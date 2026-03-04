package gift.auth;

import gift.TestFixtures;
import gift.member.Member;
import gift.member.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KakaoAuthServiceTest {

    @Mock
    private KakaoLoginProperties properties;

    @Mock
    private KakaoLoginClient kakaoLoginClient;

    @Mock
    private MemberRepository memberRepository;

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
        given(memberRepository.findByEmail("user@kakao.com")).willReturn(Optional.of(member));
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));
        given(jwtProvider.createToken("user@kakao.com")).willReturn("jwt-token");

        var result = kakaoAuthService.loginOrRegister("code123");

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(member.getKakaoAccessToken()).isEqualTo("kakao-token");
    }

    @Test
    void loginOrRegister_newMember_createsAndReturnsJwt() {
        given(kakaoLoginClient.requestAccessToken("code123"))
            .willReturn(new KakaoLoginClient.KakaoTokenResponse("kakao-token"));
        given(kakaoLoginClient.requestUserInfo("kakao-token"))
            .willReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("new@kakao.com")));
        given(memberRepository.findByEmail("new@kakao.com")).willReturn(Optional.empty());
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));
        given(jwtProvider.createToken("new@kakao.com")).willReturn("jwt-token");

        var result = kakaoAuthService.loginOrRegister("code123");

        assertThat(result.token()).isEqualTo("jwt-token");
        then(memberRepository).should().save(any(Member.class));
    }

    @Test
    void loginOrRegister_savesKakaoAccessToken() {
        var member = TestFixtures.member(1L, "user@kakao.com", null);
        given(kakaoLoginClient.requestAccessToken("code123"))
            .willReturn(new KakaoLoginClient.KakaoTokenResponse("new-kakao-token"));
        given(kakaoLoginClient.requestUserInfo("new-kakao-token"))
            .willReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("user@kakao.com")));
        given(memberRepository.findByEmail("user@kakao.com")).willReturn(Optional.of(member));
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));
        given(jwtProvider.createToken("user@kakao.com")).willReturn("jwt");

        kakaoAuthService.loginOrRegister("code123");

        assertThat(member.getKakaoAccessToken()).isEqualTo("new-kakao-token");
    }

    @Test
    void loginOrRegister_generatesJwtWithEmail() {
        given(kakaoLoginClient.requestAccessToken("code123"))
            .willReturn(new KakaoLoginClient.KakaoTokenResponse("kakao-token"));
        given(kakaoLoginClient.requestUserInfo("kakao-token"))
            .willReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("test@kakao.com")));
        given(memberRepository.findByEmail("test@kakao.com")).willReturn(Optional.empty());
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));
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
