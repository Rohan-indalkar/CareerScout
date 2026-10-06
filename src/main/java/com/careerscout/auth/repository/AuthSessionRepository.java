package com.careerscout.auth.repository;

import com.careerscout.auth.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
    Optional<AuthSession> findByIdAndUserId(String id, Long userId);
    List<AuthSession> findAllByUserIdAndRevokedAtIsNull(Long userId);
}
