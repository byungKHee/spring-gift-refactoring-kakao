package gift.order;

import gift.TestFixtures;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private KakaoMessageClient kakaoMessageClient;

    @InjectMocks
    private OrderService orderService;

    private Member member;
    private Option option;

    @BeforeEach
    void setUp() {
        member = TestFixtures.member(1L, "test@test.com", "password");
        member.chargePoint(100000);
        option = TestFixtures.option(1L, TestFixtures.product(), "기본 옵션", 100);
    }

    @Test
    void placeOrder_happyPath_completesAllSteps() {
        var request = new OrderRequest(1L, 2, "선물입니다");
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        var result = orderService.placeOrder(member, request);

        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(2);
        then(orderRepository).should().save(any(Order.class));
    }

    @Test
    void placeOrder_optionNotFound_throws() {
        var request = new OrderRequest(99L, 1, null);
        given(optionRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(member, request))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void placeOrder_insufficientStock_throws() {
        var request = new OrderRequest(1L, 999, null);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));

        assertThatThrownBy(() -> orderService.placeOrder(member, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("재고");
    }

    @Test
    void placeOrder_insufficientPoints_throws() {
        var poorMember = TestFixtures.member(2L, "poor@test.com", "pw");
        var request = new OrderRequest(1L, 1, null);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));

        assertThatThrownBy(() -> orderService.placeOrder(poorMember, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("포인트");
    }

    @Test
    void placeOrder_savesOrderWithCorrectFields() {
        var request = new OrderRequest(1L, 3, "축하합니다");
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        var result = orderService.placeOrder(member, request);

        assertThat(result.getOption()).isEqualTo(option);
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getQuantity()).isEqualTo(3);
        assertThat(result.getMessage()).isEqualTo("축하합니다");
    }

    @Test
    void placeOrder_calculatesCorrectPrice() {
        var request = new OrderRequest(1L, 5, null);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(member, request);

        // price = 10000 * 5 = 50000, 초기 100000 - 50000 = 50000
        assertThat(member.getPoint()).isEqualTo(50000);
    }

    @Test
    void placeOrder_withKakaoToken_sendsNotification() {
        var kakaoMember = TestFixtures.kakaoMember(1L, "kakao@test.com", "kakao-token");
        kakaoMember.chargePoint(100000);
        var request = new OrderRequest(1L, 1, null);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(kakaoMember, request);

        then(kakaoMessageClient).should().sendToMe(eq("kakao-token"), any(Order.class), any());
    }

    @Test
    void placeOrder_withoutKakaoToken_skipsNotification() {
        var request = new OrderRequest(1L, 1, null);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(member, request);

        then(kakaoMessageClient).should(never()).sendToMe(any(), any(), any());
    }

    @Test
    void placeOrder_notificationFails_orderStillSucceeds() {
        var kakaoMember = TestFixtures.kakaoMember(1L, "kakao@test.com", "kakao-token");
        kakaoMember.chargePoint(100000);
        var request = new OrderRequest(1L, 1, null);
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("알림 실패")).when(kakaoMessageClient)
            .sendToMe(any(), any(), any());

        var result = orderService.placeOrder(kakaoMember, request);

        assertThat(result).isNotNull();
    }

    @Test
    void findByMemberId_returnsPagedOrders() {
        var order = TestFixtures.order(1L, option, 1L, 2, "메시지");
        var pageable = PageRequest.of(0, 10);
        given(orderRepository.findByMemberId(1L, pageable)).willReturn(new PageImpl<>(List.of(order)));

        var result = orderService.findByMemberId(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }
}
