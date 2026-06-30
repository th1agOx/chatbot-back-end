package br.com.chatbot.chatbot_api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenProviderTest {

    private TokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        var secret = "dGhpcy1pcy1hLXNlY3JldC1rZXktZm9yLXRlc3Rpbmctb25seS1hbmQtbXVzdC1iZS1hdC1sZWFzdC0yNTYtYml0cw==";
        tokenProvider = new TokenProvider(secret, 86400000L);
    }

    @Test
    void createToken_ShouldReturnValidToken() {
        var token = tokenProvider.createToken("teste@email.com");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void getEmailFromToken_ValidToken_ReturnsEmail() {
        var token = tokenProvider.createToken("teste@email.com");
        var email = tokenProvider.getEmailFromToken(token);
        assertTrue(email.isPresent());
        assertEquals("teste@email.com", email.get());
    }

    @Test
    void getEmailFromToken_InvalidToken_ReturnsEmpty() {
        var email = tokenProvider.getEmailFromToken("invalid.token.jwt");
        assertTrue(email.isEmpty());
    }

    @Test
    void getEmailFromToken_ExpiredToken_ReturnsEmpty() {
        var shortTokenProvider = new TokenProvider(
                "dGhpcy1pcy1hLXNlY3JldC1rZXktZm9yLXRlc3Rpbmctb25seS1hbmQtbXVzdC1iZS1hdC1sZWFzdC0yNTYtYml0cw==",
                -1L);
        var token = shortTokenProvider.createToken("teste@email.com");
        var email = shortTokenProvider.getEmailFromToken(token);
        assertTrue(email.isEmpty());
    }
}
