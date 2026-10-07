package com.careerscout.career.service;

import com.careerscout.career.crawler.CareerJobExtractor;
import com.careerscout.career.crawler.CareerPageFetcher;
import com.careerscout.career.dto.CareerSourceScanResponse;
import com.careerscout.career.entity.CareerSource;
import com.careerscout.career.repository.CareerSourceRepository;
import com.careerscout.common.exception.BadRequestException;
import com.careerscout.common.exception.ResourceNotFoundException;
import com.careerscout.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CareerPageScanService {
    private final CareerSourceRepository careerSources;
    private final CareerPageFetcher pageFetcher;
    private final CareerJobExtractor jobExtractor;
    private final JobIngestionService jobIngestion;
    private final CareerSourceScanGuard scanGuard;

    public CareerPageScanService(CareerSourceRepository careerSources, CareerPageFetcher pageFetcher,
                                 CareerJobExtractor jobExtractor, JobIngestionService jobIngestion,
                                 CareerSourceScanGuard scanGuard) {
        this.careerSources = careerSources;
        this.pageFetcher = pageFetcher;
        this.jobExtractor = jobExtractor;
        this.jobIngestion = jobIngestion;
        this.scanGuard = scanGuard;
    }

    public CareerSourceScanResponse scan(AuthenticatedUser user, Long sourceId) {
        CareerSource source = careerSources.findByIdAndUserId(sourceId, user.id())
                .orElseThrow(() -> new ResourceNotFoundException("Career source not found"));
        if (!source.isActive()) {
            throw new BadRequestException("Career source is inactive");
        }
        scanGuard.claim(source.getId(), source.getCareerUrl(), source.getCompanyName(), Instant.now(), false);
        return fetchAndIngest(source);
    }

    public void scanScheduled(Long sourceId) {
        CareerSource source = careerSources.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Career source not found"));
        if (!source.isActive()) return;
        if (!scanGuard.claim(source.getId(), source.getCareerUrl(), source.getCompanyName(),
                Instant.now(), true)) return;
        fetchAndIngest(source);
    }

    private CareerSourceScanResponse fetchAndIngest(CareerSource source) {
        Long sourceId = source.getId();
        String sourceUrl = source.getCareerUrl();
        String companyName = source.getCompanyName();
        String html = pageFetcher.fetch(sourceUrl);
        var extractedJobs = jobExtractor.extract(html, companyName, sourceUrl);
        return jobIngestion.ingest(sourceId, sourceUrl, companyName, extractedJobs);
    }
}
