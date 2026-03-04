package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationResolver {
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    public AuthenticationResolver(JwtProvider jwtProvider, MemberRepository memberRepository) {
        this.jwtProvider = jwtProvider;
        this.memberRepository = memberRepository;
    }

    public Member extractMember(String authorization) {
        try {
            final String token = authorization.replace("Bearer ", "");
            final String email = jwtProvider.getEmail(token);
            return memberRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("인증된 사용자를 찾을 수 없습니다."));
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("유효하지 않은 인증 정보입니다.");
        }
    }
}
