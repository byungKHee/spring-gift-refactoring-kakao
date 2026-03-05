package gift.auth;

import gift.member.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static gift.TestFixtures.member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthenticationResolverTest {
    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AuthenticationResolver authenticationResolver;

    @Test
    void extractMember_validToken_returnsMember() {
        var m = member();
        given(jwtProvider.getEmail("valid-token")).willReturn("test@test.com");
        given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(m));

        var result = authenticationResolver.extractMember("Bearer valid-token");

        assertThat(result.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void extractMember_invalidToken_throwsUnauthorized() {
        given(jwtProvider.getEmail("bad-token")).willThrow(new RuntimeException("invalid"));

        assertThatThrownBy(() -> authenticationResolver.extractMember("Bearer bad-token"))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("유효하지 않은 인증 정보입니다.");
    }

    @Test
    void extractMember_noMemberFound_throwsUnauthorized() {
        given(jwtProvider.getEmail("orphan-token")).willReturn("orphan@test.com");
        given(memberRepository.findByEmail("orphan@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationResolver.extractMember("Bearer orphan-token"))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("인증된 사용자를 찾을 수 없습니다.");
    }

    @Test
    void extractMember_nullAuthorization_throwsUnauthorized() {
        assertThatThrownBy(() -> authenticationResolver.extractMember(null))
            .isInstanceOf(UnauthorizedException.class);
    }
}
