package gift.member;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public MemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member findById(Long id) {
        return memberRepository.findById(id).orElseThrow();
    }

    @Transactional
    public TokenResponse register(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }
        Member member = memberRepository.save(new Member(email, password));
        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }

    public TokenResponse login(String email, String password) {
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (member.getPassword() == null || !member.getPassword().equals(password)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }

    @Transactional
    public Member create(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }
        return memberRepository.save(new Member(email, password));
    }

    @Transactional
    public Member update(Long id, String email, String password) {
        Member member = memberRepository.findById(id).orElseThrow();
        member.update(email, password);
        return member;
    }

    @Transactional
    public void chargePoint(Long id, int amount) {
        Member member = memberRepository.findById(id).orElseThrow();
        member.chargePoint(amount);
    }

    @Transactional
    public void delete(Long id) {
        memberRepository.deleteById(id);
    }
}
