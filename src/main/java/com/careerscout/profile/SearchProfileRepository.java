package com.careerscout.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SearchProfileRepository extends JpaRepository<SearchProfile, Long> {
    List<SearchProfile> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    List<SearchProfile> findAllByUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);
    Optional<SearchProfile> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndNormalizedName(Long userId, String normalizedName);
    boolean existsByUserIdAndNormalizedNameAndIdNot(Long userId, String normalizedName, Long id);
}
