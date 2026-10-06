package com.careerscout.auth.service;

import com.careerscout.auth.dto.AuthResponse;
import com.careerscout.auth.dto.LoginRequest;
import com.careerscout.auth.dto.RegisterRequest;
import com.careerscout.auth.entity.AuthSession;
import com.careerscout.auth.entity.RefreshToken;
import com.careerscout.auth.repository.AuthSessionRepository;
import com.careerscout.auth.repository.RefreshTokenRepository;
import com.careerscout.common.exception.DuplicateResourceException;
import com.careerscout.common.exception.UnauthorizedException;
import com.careerscout.security.JwtService;
import com.careerscout.user.UserAccount;
import com.careerscout.user.UserRepository;
import com.careerscout.user.dto.UserResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository users, RefreshTokenRepository refreshTokens,
                           AuthSessionRepository sessions,
                           PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        validateBcryptLength(request.password());
        UserAccount user = createUser(request, email);
        return issueResponse(user, null);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserAccount user = users.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(UserAccount::isEnabled)
                .filter(account -> passwordEncoder.matches(request.password(), account.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return issueResponse(user, null);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        Claims claims = parseToken(refreshToken);
        if (!jwtService.isTokenType(claims, "refresh")) {
            throw invalidRefreshToken();
        }
        Long userId = userId(claims);
        RefreshToken stored = refreshTokens.findByTokenIdAndUserId(claims.getId(), userId)
                .orElseThrow(AuthServiceImpl::invalidRefreshToken);
        if (!stored.isUsableAt(Instant.now()) || !constantTimeEquals(stored.getTokenHash(), sha256(refreshToken))) {
            throw invalidRefreshToken();
        }
        String sessionId = claims.get("sid", String.class);
        if (sessionId == null || !sessionId.equals(stored.getSessionId())
                || !sessions.findByIdAndUserId(sessionId, userId)
                .filter(session -> session.isActiveAt(Instant.now())).isPresent()) {
            throw invalidRefreshToken();
        }
        UserAccount user = users.findById(userId)
                .filter(UserAccount::isEnabled)
                .orElseThrow(AuthServiceImpl::invalidRefreshToken);
        stored.revoke();
        return issueResponse(user, sessionId);
    }

    @Override
    @Transactional
    public void logout(Long userId, String sessionId, String refreshToken) {
        Claims refreshClaims = parseToken(refreshToken);
        if (!jwtService.isTokenType(refreshClaims, "refresh") || !userId.equals(userId(refreshClaims))) {
            throw invalidRefreshToken();
        }
        RefreshToken stored = refreshTokens.findByTokenIdAndUserId(refreshClaims.getId(), userId)
                .orElseThrow(AuthServiceImpl::invalidRefreshToken);
        if (!stored.isUsableAt(Instant.now()) || !stored.getSessionId().equals(sessionId)
                || !constantTimeEquals(stored.getTokenHash(), sha256(refreshToken))) {
            throw invalidRefreshToken();
        }
        stored.revoke();
        AuthSession session = sessions.findByIdAndUserId(stored.getSessionId(), userId)
                .filter(value -> value.isActiveAt(Instant.now()))
                .orElseThrow(AuthServiceImpl::invalidRefreshToken);
        session.revoke();
    }

    @Override
    @Transactional
    public void revokeSessions(Long userId) {
        sessions.findAllByUserIdAndRevokedAtIsNull(userId).forEach(AuthSession::revoke);
    }

    private UserAccount createUser(RegisterRequest request, String email) {
        try {
            return users.saveAndFlush(new UserAccount(request.name().trim(), email,
                    passwordEncoder.encode(request.password()), com.careerscout.user.Role.USER));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
    }

    private AuthResponse issueResponse(UserAccount user, String sessionId) {
        JwtService.TokenPair pair = jwtService.issueTokens(user, sessionId);
        if (sessionId == null) {
            sessions.save(new AuthSession(pair.sessionId(), user, pair.refreshExpiresAt()));
        } else {
            sessions.findById(pair.sessionId()).orElseThrow(AuthServiceImpl::invalidRefreshToken)
                    .updateExpiry(pair.refreshExpiresAt());
        }
        refreshTokens.save(new RefreshToken(pair.refreshTokenId(), user, pair.sessionId(), sha256(pair.refreshToken()),
                pair.refreshExpiresAt()));
        return new AuthResponse(pair.accessToken(), pair.refreshToken(), "Bearer", pair.accessExpiresAt(),
                pair.refreshExpiresAt(), toResponse(user));
    }

    private static UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getCreatedAt(), user.getUpdatedAt());
    }

    private Claims parseToken(String token) {
        try {
            return jwtService.parse(token);
        } catch (JwtException | IllegalArgumentException exception) {
            throw invalidRefreshToken();
        }
    }

    private static Long userId(Claims claims) {
        Object value = claims.get("uid");
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw invalidRefreshToken();
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static void validateBcryptLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new com.careerscout.common.exception.BadRequestException(
                    "Password must be no more than 72 UTF-8 bytes");
        }
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static boolean constantTimeEquals(String first, String second) {
        return MessageDigest.isEqual(first.getBytes(StandardCharsets.US_ASCII),
                second.getBytes(StandardCharsets.US_ASCII));
    }

    private static UnauthorizedException invalidRefreshToken() {
        return new UnauthorizedException("Refresh token is invalid, expired, or revoked");
    }
}
