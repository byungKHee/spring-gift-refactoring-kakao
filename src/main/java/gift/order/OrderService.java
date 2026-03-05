package gift.order;

import gift.member.Member;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final KakaoMessageClient kakaoMessageClient;
    private final TransactionTemplate transactionTemplate;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        KakaoMessageClient kakaoMessageClient,
        TransactionTemplate transactionTemplate
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.kakaoMessageClient = kakaoMessageClient;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(readOnly = true)
    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    public Order placeOrder(Member member, OrderRequest request) {
        var holder = transactionTemplate.execute(status -> {
            Option option = optionRepository.findById(request.optionId()).orElseThrow();
            option.subtractQuantity(request.quantity());

            int price = option.getProduct().getPrice() * request.quantity();
            member.deductPoint(price);

            Order saved = orderRepository.save(
                new Order(option, member.getId(), request.quantity(), request.message()));
            return new OrderHolder(saved, option.getProduct());
        });

        sendKakaoMessageIfPossible(member, holder.order(), holder.product());
        return holder.order();
    }

    private void sendKakaoMessageIfPossible(Member member, Order order, Product product) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception e) {
            log.warn("카카오 메시지 전송 실패", e);
        }
    }

    private record OrderHolder(Order order, Product product) {
    }
}
