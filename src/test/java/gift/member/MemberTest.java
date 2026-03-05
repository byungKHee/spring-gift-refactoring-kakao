package gift.member;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {
    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member("test@test.com", "password");
    }

    @Test
    void chargePoint_positiveAmount_addsToBalance() {
        member.chargePoint(1000);
        assertThat(member.getPoint()).isEqualTo(1000);
    }

    @Test
    void chargePoint_zero_throwsException() {
        assertThatThrownBy(() -> member.chargePoint(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("1 이상");
    }

    @Test
    void chargePoint_negativeAmount_throwsException() {
        assertThatThrownBy(() -> member.chargePoint(-100))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deductPoint_sufficientBalance_deducts() {
        member.chargePoint(1000);
        member.deductPoint(500);
        assertThat(member.getPoint()).isEqualTo(500);
    }

    @Test
    void deductPoint_exactBalance_deductsToZero() {
        member.chargePoint(1000);
        member.deductPoint(1000);
        assertThat(member.getPoint()).isEqualTo(0);
    }

    @Test
    void deductPoint_insufficientBalance_throwsException() {
        member.chargePoint(100);
        assertThatThrownBy(() -> member.deductPoint(200))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("포인트가 부족합니다.");
    }

    @Test
    void deductPoint_zero_throwsException() {
        assertThatThrownBy(() -> member.deductPoint(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("1 이상");
    }

    @Test
    void deductPoint_negativeAmount_throwsException() {
        assertThatThrownBy(() -> member.deductPoint(-100))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
