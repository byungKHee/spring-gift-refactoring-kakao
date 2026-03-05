package gift.auth;

import gift.member.Member;
import gift.member.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class KakaoAuthService {
    private final KakaoLoginProperties properties;
    private final KakaoLoginClient kakaoLoginClient;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;
    private final TransactionTemplate transactionTemplate;

    public KakaoAuthService(
        KakaoLoginProperties properties,
        KakaoLoginClient kakaoLoginClient,
        MemberService memberService,
        JwtProvider jwtProvider,
        TransactionTemplate transactionTemplate
    ) {
        this.properties = properties;
        this.kakaoLoginClient = kakaoLoginClient;
        this.memberService = memberService;
        this.jwtProvider = jwtProvider;
        this.transactionTemplate = transactionTemplate;
    }

    public String buildAuthorizationUrl() {
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", properties.clientId())
            .queryParam("redirect_uri", properties.redirectUri())
            .queryParam("scope", "account_email,talk_message")
            .build()
            .toUriString();
    }

    public TokenResponse loginOrRegister(String code) {
        KakaoLoginClient.KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);
        KakaoLoginClient.KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(kakaoToken.accessToken());
        String email = kakaoUser.email();

        Member member = transactionTemplate.execute(status -> {
            Member m = memberService.findOrCreateByEmail(email);
            m.updateKakaoAccessToken(kakaoToken.accessToken());
            return m;
        });

        return new TokenResponse(jwtProvider.createToken(member.getEmail()));
    }
}
