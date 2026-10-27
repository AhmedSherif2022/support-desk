package com.supportdesk.auth.service;
import com.supportdesk.auth.config.AuthProperties;
import com.supportdesk.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;


@Service
public class JwtService
{


    public record IssuedToken(String token, Instant expiresAt)
    {
    }

    public record VerifiedToken(String username, int tokenVersion)
    {
    }
    private final SecretKey signingKey;
    private final Duration sessionTimeout;
    private final Clock clock;

    public JwtService(AuthProperties properties, Clock clock)
    {
        byte[] keyBytes = Decoders.BASE64.decode(properties.jwtSecret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes); // fails at startup if the key is shorter than 256 bits
        this.sessionTimeout = properties.sessionTimeout();
        this.clock = clock;
    }



    public IssuedToken generateToken(User user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(sessionTimeout);

        String token = Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .claim("tv", user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();

        return new IssuedToken(token, expiresAt);
    }


    public Optional<VerifiedToken> validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Integer tokenVersion = claims.get("tv", Integer.class);
            // Tokens issued before token versioning have no "tv" claim and are treated as invalid.
            if (claims.getSubject() == null || tokenVersion == null) {
                return Optional.empty();
            }
            return Optional.of(new VerifiedToken(claims.getSubject(), tokenVersion));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
