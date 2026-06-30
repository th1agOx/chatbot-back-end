package br.com.chatbot.chatbot_api.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Configuration
public class TokenProvider {

    private final SecretKey secretKey;
    private final long tokenValidityMs;

    public TokenProvider(
            @Value("${api.security.token.secret}") String secret,
            @Value("${api.security.token.expiration:86400000}") long validityMs) {
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
