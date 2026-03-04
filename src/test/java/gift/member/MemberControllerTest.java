package gift.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.auth.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
class MemberControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @Test
    void register_valid_returns201() throws Exception {
        given(memberService.register("test@test.com", "password"))
            .willReturn(new TokenResponse("jwt-token"));
        var body = new MemberRequest("test@test.com", "password");

        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void register_duplicateEmail_returns400() throws Exception {
        given(memberService.register("dup@test.com", "password"))
            .willThrow(new IllegalArgumentException("이미 등록된 이메일입니다."));
        var body = new MemberRequest("dup@test.com", "password");

        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void login_valid_returns200() throws Exception {
        given(memberService.login("test@test.com", "password"))
            .willReturn(new TokenResponse("jwt-token"));
        var body = new MemberRequest("test@test.com", "password");

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_invalidCredentials_returns400() throws Exception {
        given(memberService.login("test@test.com", "wrong"))
            .willThrow(new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        var body = new MemberRequest("test@test.com", "wrong");

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
