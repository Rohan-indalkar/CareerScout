package com.careerscout.security;

import com.careerscout.user.UserAccount;
import com.careerscout.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final Duration accessLifetime;
    private final Duration refreshLifetime;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration:PT15M}") Duration accessLifetime,
            @Value("${app.jwt.refresh-token-expiration:P7D}") Duration refreshLifetime) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        }
        if (accessLifetime.isZero() || accessLifetime.isNegative()
                || refreshLifetime.isZero() || refreshLifetime.isNegative()) {
            throw new IllegalArgumentException("JWT token lifetimes must be positive");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessLifetime = accessLifetime;
        this.refreshLifetime = refreshLifetime;
    }

    public TokenPair issueTokens(UserAccount user, String requestedSessionId) {
        Instant now = Instant.now();
        String accessId = UUID.randomUUID().toString();
        String refreshId = UUID.randomUUID().toString();
        String sessionId = requestedSessionId == null ? UUID.randomUUID().toString() : requestedSessionId;
        Instant accessExpiry = now.plus(accessLifetime);
        Instant refreshExpiry = now.plus(refreshLifetime);
        String accessToken = createToken(user, "access", accessId, sessionId, now, accessExpiry);
        String refreshToken = createToken(user, "refresh", refreshId, sessionId, now, refreshExpiry);
        return new TokenPair(accessToken, refreshToken, accessId, refreshId, sessionId,
                accessExpiry, refreshExpiry);
    }

    private String createToken(UserAccount user, String type, String tokenId, String sessionId,
                               Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .id(tokenId)
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .claim("type", type)
                .claim("sid", sessionId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
    }

    public boolean isTokenType(Claims claims, String type) {
        return type.equals(claims.get("type", String.class));
    }

    public Optional<Claims> parseAccessToken(String token) {
        try {
            Claims claims = parse(token);
            return isTokenType(claims, "access") ? Optional.of(claims) : Optional.empty();
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public Duration getAccessLifetime() { return accessLifetime; }

    public record TokenPair(String accessToken, String refreshToken, String accessTokenId,
                            String refreshTokenId, String sessionId,
                            Instant accessExpiresAt, Instant refreshExpiresAt) {
    }
}
