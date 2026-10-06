package com.careerscout.career.repository;

import com.careerscout.career.entity.CareerSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CareerSourceRepository extends JpaRepository<CareerSource, Long> {
    List<CareerSource> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<CareerSource> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndNormalizedUrl(Long userId, String normalizedUrl);
    boolean existsByUserIdAndNormalizedUrlAndIdNot(Long userId, String normalizedUrl, Long id);
}
