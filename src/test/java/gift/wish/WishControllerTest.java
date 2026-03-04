package gift.wish;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.auth.AuthenticationResolver;
import gift.auth.UnauthorizedException;
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
import static gift.TestFixtures.product;
import static gift.TestFixtures.wish;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WishController.class)
class WishControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WishService wishService;

    @MockitoBean
    private AuthenticationResolver authenticationResolver;

    @Test
    void getWishes_authenticated_returns200() throws Exception {
        var m = member();
        given(authenticationResolver.extractMember("Bearer valid-token")).willReturn(m);
        given(wishService.findByMemberId(eq(m.getId()), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(wish(1L, m.getId(), product()))));

        mockMvc.perform(get("/api/wishes").header("Authorization", "Bearer valid-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].productId").value(1));
    }

    @Test
    void getWishes_unauthenticated_returns401() throws Exception {
        given(authenticationResolver.extractMember("Bearer invalid"))
            .willThrow(new UnauthorizedException("유효하지 않은 인증 정보입니다."));

        mockMvc.perform(get("/api/wishes").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void addWish_new_returns201() throws Exception {
        var m = member();
        given(authenticationResolver.extractMember("Bearer valid-token")).willReturn(m);
        var w = wish(1L, m.getId(), product());
        given(wishService.add(m.getId(), 1L)).willReturn(new WishService.AddResult(w, true));
        var body = new WishRequest(1L);

        mockMvc.perform(post("/api/wishes")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.productId").value(1));
    }

    @Test
    void addWish_duplicate_returns200() throws Exception {
        var m = member();
        given(authenticationResolver.extractMember("Bearer valid-token")).willReturn(m);
        var w = wish(1L, m.getId(), product());
        given(wishService.add(m.getId(), 1L)).willReturn(new WishService.AddResult(w, false));
        var body = new WishRequest(1L);

        mockMvc.perform(post("/api/wishes")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId").value(1));
    }

    @Test
    void removeWish_returns204() throws Exception {
        var m = member();
        given(authenticationResolver.extractMember("Bearer valid-token")).willReturn(m);
        willDoNothing().given(wishService).remove(m.getId(), 1L);

        mockMvc.perform(delete("/api/wishes/1")
                .header("Authorization", "Bearer valid-token"))
            .andExpect(status().isNoContent());
    }
}
