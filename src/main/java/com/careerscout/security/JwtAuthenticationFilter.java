package com.careerscout.security;

import com.careerscout.auth.entity.AuthSession;
import com.careerscout.auth.repository.AuthSessionRepository;
import com.careerscout.user.UserRepository;
import com.careerscout.user.UserAccount;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository users;
    private final AuthSessionRepository sessions;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users,
                                   AuthSessionRepository sessions) {
        this.jwtService = jwtService;
        this.users = users;
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.parseAccessToken(authorization.substring(7)).ifPresent(claims -> authenticate(claims));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(Claims claims) {
        Object userIdClaim = claims.get("uid");
        String sessionId = claims.get("sid", String.class);
        if (!(userIdClaim instanceof Number userId) || claims.getId() == null
                || claims.getSubject() == null || claims.getExpiration() == null || sessionId == null) {
            return;
        }
        AuthSession session = sessions.findByIdAndUserId(sessionId, userId.longValue())
                .filter(value -> value.isActiveAt(java.time.Instant.now()))
                .orElse(null);
        if (session == null) return;
        users.findById(userId.longValue())
                .filter(UserAccount::isEnabled)
                .filter(user -> user.getEmail().equalsIgnoreCase(claims.getSubject()))
                .ifPresent(user -> {
                    AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(),
                            user.getPasswordHash(), user.getRole(), sessionId);
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
    }
}
