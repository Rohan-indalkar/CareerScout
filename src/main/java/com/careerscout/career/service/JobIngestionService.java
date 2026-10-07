package com.careerscout.career.service;

import com.careerscout.career.crawler.ExtractedJob;
import com.careerscout.career.dto.CareerSourceScanResponse;
import com.careerscout.career.entity.CareerSource;
import com.careerscout.career.repository.CareerSourceRepository;
import com.careerscout.common.exception.BadRequestException;
import com.careerscout.common.exception.ResourceNotFoundException;
import com.careerscout.job.entity.Job;
import com.careerscout.job.entity.JobMatch;
import com.careerscout.job.repository.JobMatchRepository;
import com.careerscout.job.repository.JobRepository;
import com.careerscout.job.service.JobMatchEvaluation;
import com.careerscout.job.service.JobMatchingEngine;
import com.careerscout.profile.SearchProfile;
import com.careerscout.profile.SearchProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class JobIngestionService {
    private final CareerSourceRepository careerSources;
    private final JobRepository jobs;
    private final JobMatchRepository matches;
    private final SearchProfileRepository profiles;
    private final JobMatchingEngine matchingEngine;

    public JobIngestionService(CareerSourceRepository careerSources, JobRepository jobs,
                               JobMatchRepository matches, SearchProfileRepository profiles,
                               JobMatchingEngine matchingEngine) {
        this.careerSources = careerSources;
        this.jobs = jobs;
        this.matches = matches;
        this.profiles = profiles;
        this.matchingEngine = matchingEngine;
    }

    @Transactional
    public CareerSourceScanResponse ingest(Long sourceId, String scannedUrl, String scannedCompanyName,
                                           List<ExtractedJob> extractedJobs) {
        CareerSource source = careerSources.findByIdForUpdate(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Career source not found"));
        if (!source.isActive()) {
            throw new BadRequestException("Career source is inactive");
        }
        if (!source.getCareerUrl().equals(scannedUrl) || !source.getCompanyName().equals(scannedCompanyName)) {
            throw new BadRequestException("Career source changed during scanning; please scan again");
        }

        Instant scannedAt = Instant.now();
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        var activeProfiles = profiles.findAllByUserIdAndActiveTrueOrderByCreatedAtDesc(source.getUser().getId());
        for (ExtractedJob extracted : extractedJobs) {
            Optional<Job> existing = extracted.externalJobId() == null
                    ? Optional.empty()
                    : jobs.findByCareerSourceIdAndExternalJobId(sourceId, extracted.externalJobId());
            if (existing.isEmpty()) {
                existing = jobs.findByCareerSourceIdAndJobUrl(sourceId, extracted.jobUrl());
            }

            Job job;
            if (existing.isPresent()) {
                job = existing.get();
                boolean changed = existing.get().refreshFromScan(extracted.externalJobId(),
                        extracted.title(), extracted.normalizedTitle(), extracted.companyName(), extracted.location(),
                        extracted.experience(), extracted.description(), extracted.jobUrl(),
                        extracted.skills(), extracted.contentHash(), scannedAt);
                if (changed) updated++;
                else unchanged++;
            } else {
                job = jobs.save(new Job(source, extracted.externalJobId(), extracted.title(),
                        extracted.normalizedTitle(), extracted.companyName(), extracted.location(), extracted.experience(),
                        extracted.description(), extracted.jobUrl(), extracted.skills(),
                        extracted.contentHash(), scannedAt));
                created++;
            }
            updateMatches(job, activeProfiles, scannedAt);
        }
        source.markScanned(scannedAt);
        return new CareerSourceScanResponse(sourceId, extractedJobs.size(), created, updated, unchanged, scannedAt);
    }

    private void updateMatches(Job job, List<SearchProfile> activeProfiles, Instant evaluatedAt) {
        for (SearchProfile profile : activeProfiles) {
            JobMatchEvaluation evaluation = matchingEngine.evaluate(job, profile);
            JobMatch match = matches.findByJobIdAndSearchProfileId(job.getId(), profile.getId())
                    .orElseGet(() -> new JobMatch(job, profile, evaluation.score(), evaluation.matched(),
                            evaluation.positionMatched(), evaluation.locationMatched(),
                            evaluation.experienceMatched(), evaluation.skillsMatched(),
                            evaluation.keywordMatched(), evaluation.explanation(),
                            evaluation.rejectionReason(), evaluatedAt));
            if (match.getId() != null) {
                match.updateEvaluation(evaluation.score(), evaluation.matched(),
                        evaluation.positionMatched(), evaluation.locationMatched(),
                        evaluation.experienceMatched(), evaluation.skillsMatched(),
                        evaluation.keywordMatched(), evaluation.explanation(),
                        evaluation.rejectionReason(), evaluatedAt);
            }
            matches.save(match);
        }
    }
}
