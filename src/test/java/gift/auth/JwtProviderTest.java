package gift.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
            "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha", 3600000L);
    }

    @Test
    void createToken_validEmail_returnsNonEmptyToken() {
        String token = jwtProvider.createToken("user@test.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void getEmail_validToken_returnsEmail() {
        String token = jwtProvider.createToken("user@test.com");
        assertThat(jwtProvider.getEmail(token)).isEqualTo("user@test.com");
    }

    @Test
    void getEmail_invalidToken_throwsException() {
        assertThatThrownBy(() -> jwtProvider.getEmail("invalid-token"))
            .isInstanceOf(Exception.class);
    }

    @Test
    void getEmail_tamperedToken_throwsException() {
        String token = jwtProvider.createToken("user@test.com");
        // Replace a character in the signature part to ensure tampering
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThatThrownBy(() -> jwtProvider.getEmail(tampered))
            .isInstanceOf(Exception.class);
    }

    @Test
    void getEmail_expiredToken_throwsException() {
        var expiredProvider = new JwtProvider(
            "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha", 0L);
        String token = expiredProvider.createToken("user@test.com");
        assertThatThrownBy(() -> expiredProvider.getEmail(token))
            .isInstanceOf(Exception.class);
    }
}
