package gift.member;

import gift.TestFixtures;
import gift.auth.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private MemberService memberService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = TestFixtures.member(1L, "test@test.com", "password");
    }

    @Test
    void register_newEmail_savesAndReturnsToken() {
        given(memberRepository.existsByEmail("new@test.com")).willReturn(false);
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));
        given(jwtProvider.createToken("new@test.com")).willReturn("jwt-token");

        var result = memberService.register("new@test.com", "pw");

        assertThat(result.token()).isEqualTo("jwt-token");
        then(memberRepository).should().save(any(Member.class));
    }

    @Test
    void register_duplicateEmail_throws() {
        given(memberRepository.existsByEmail("test@test.com")).willReturn(true);

        assertThatThrownBy(() -> memberService.register("test@test.com", "pw"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 등록된 이메일");
    }

    @Test
    void login_validCredentials_returnsToken() {
        given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(member));
        given(jwtProvider.createToken("test@test.com")).willReturn("jwt-token");

        var result = memberService.login("test@test.com", "password");

        assertThat(result.token()).isEqualTo("jwt-token");
    }

    @Test
    void login_emailNotFound_throws() {
        given(memberRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.login("unknown@test.com", "pw"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void login_wrongPassword_throws() {
        given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.login("test@test.com", "wrong"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findAll_returnsList() {
        given(memberRepository.findAll()).willReturn(List.of(member));

        var result = memberService.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void findById_existing_returns() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        var result = memberService.findById(1L);

        assertThat(result.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void findById_notFound_throws() {
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.findById(99L))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void update_updatesFields() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        var result = memberService.update(1L, "updated@test.com", "newpw");

        assertThat(result.getEmail()).isEqualTo("updated@test.com");
    }

    @Test
    void chargePoint_validAmount_updates() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.chargePoint(1L, 1000);

        assertThat(member.getPoint()).isEqualTo(1000);
    }

    @Test
    void delete_delegatesToRepository() {
        memberService.delete(1L);

        then(memberRepository).should().deleteById(1L);
    }
}
