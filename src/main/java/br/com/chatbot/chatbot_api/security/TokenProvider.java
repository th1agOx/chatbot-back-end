package br.com.chatbot.chatbot_api.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class TokenProvider {

    private final SecretKey secretKey;
    private final long tokenValidityMs;

    public TokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.token-validity:86400000}") long validityMs) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.tokenValidityMs = validityMs;
    }

    public String createToken(String email) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(tokenValidityMs)))
                .signWith(secretKey)
                .compact();
    }

    public Optional<String> getEmailFromToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return Optional.of(claims.getPayload().getSubject());
        } catch (JwtException e) {
            return Optional.empty();
        }
    }
}
