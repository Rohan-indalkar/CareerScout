package com.careerscout.career.service;

import com.careerscout.career.entity.CareerSource;
import com.careerscout.career.repository.CareerSourceRepository;
import com.careerscout.common.exception.BadRequestException;
import com.careerscout.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CareerSourceScanGuard {
    private final CareerSourceRepository careerSources;

    public CareerSourceScanGuard(CareerSourceRepository careerSources) {
        this.careerSources = careerSources;
    }

    @Transactional
    public boolean claim(Long sourceId, String expectedUrl, String expectedCompanyName,
                         Instant attemptedAt, boolean scheduled) {
        CareerSource source = careerSources.findByIdForUpdate(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Career source not found"));
        if (!source.isActive()) {
            if (scheduled) return false;
            throw new BadRequestException("Career source is inactive");
        }
        if (!source.getCareerUrl().equals(expectedUrl)
                || !source.getCompanyName().equals(expectedCompanyName)) {
            if (scheduled) return false;
            throw new BadRequestException("Career source changed before scanning; please scan again");
        }

        Instant lastAttempt = source.getLastScanAttemptAt();
        if (scheduled && lastAttempt != null
                && attemptedAt.isBefore(lastAttempt.plusSeconds(source.getScanIntervalMinutes() * 60L))) {
            return false;
        }
        source.markScanAttempted(attemptedAt);
        return true;
    }
}
