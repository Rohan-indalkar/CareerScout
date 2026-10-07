package com.careerscout.career.repository;

import com.careerscout.career.entity.CareerSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface CareerSourceRepository extends JpaRepository<CareerSource, Long> {
    List<CareerSource> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    List<CareerSource> findAllByActiveTrueOrderByIdAsc();
    Optional<CareerSource> findByIdAndUserId(Long id, Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select source from CareerSource source where source.id = :id")
    Optional<CareerSource> findByIdForUpdate(@Param("id") Long id);
    boolean existsByUserIdAndNormalizedUrl(Long userId, String normalizedUrl);
    boolean existsByUserIdAndNormalizedUrlAndIdNot(Long userId, String normalizedUrl, Long id);
}
