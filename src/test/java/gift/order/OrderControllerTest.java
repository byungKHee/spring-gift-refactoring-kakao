package gift.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.auth.AuthenticationResolver;
import gift.auth.UnauthorizedException;
import gift.member.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static gift.TestFixtures.member;
import static gift.TestFixtures.option;
import static gift.TestFixtures.order;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private AuthenticationResolver authenticationResolver;

    @Test
    void getOrders_authenticated_returns200() throws Exception {
        var m = member();
        given(authenticationResolver.extractMember("Bearer valid-token")).willReturn(m);
        given(orderService.findByMemberId(eq(m.getId()), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(order(1L, option(), m.getId(), 1, "메시지"))));

        mockMvc.perform(get("/api/orders").header("Authorization", "Bearer valid-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].quantity").value(1));
    }

    @Test
    void getOrders_unauthenticated_returns401() throws Exception {
        given(authenticationResolver.extractMember("Bearer invalid"))
            .willThrow(new UnauthorizedException("유효하지 않은 인증 정보입니다."));

        mockMvc.perform(get("/api/orders").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void createOrder_authenticated_returns201() throws Exception {
        var m = member();
        given(authenticationResolver.extractMember("Bearer valid-token")).willReturn(m);
        given(orderService.placeOrder(any(Member.class), any(OrderRequest.class)))
            .willReturn(order(1L, option(), m.getId(), 2, "선물"));
        var body = new OrderRequest(1L, 2, "선물");

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void createOrder_unauthenticated_returns401() throws Exception {
        given(authenticationResolver.extractMember("Bearer bad"))
            .willThrow(new UnauthorizedException("유효하지 않은 인증 정보입니다."));
        var body = new OrderRequest(1L, 1, "");

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer bad")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_invalidBody_returns400() throws Exception {
        var m = member();
        given(authenticationResolver.extractMember("Bearer valid-token")).willReturn(m);
        var body = "{\"quantity\": 0}";

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
