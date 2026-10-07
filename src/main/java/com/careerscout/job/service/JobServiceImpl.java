package com.careerscout.job.service;

import com.careerscout.common.exception.BadRequestException;
import com.careerscout.common.exception.ResourceNotFoundException;
import com.careerscout.job.dto.JobMatchResponse;
import com.careerscout.job.dto.JobResponse;
import com.careerscout.job.entity.Job;
import com.careerscout.job.entity.JobMatch;
import com.careerscout.job.repository.JobMatchRepository;
import com.careerscout.job.repository.JobRepository;
import com.careerscout.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static com.careerscout.job.service.JobMatchSpecifications.*;
import static com.careerscout.job.service.JobSpecifications.*;

@Service
public class JobServiceImpl implements JobService {
    private static final int MAX_PAGE_SIZE = 100;

    private final JobRepository jobs;
    private final JobMatchRepository matches;

    public JobServiceImpl(JobRepository jobs, JobMatchRepository matches) {
        this.jobs = jobs;
        this.matches = matches;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> findJobs(AuthenticatedUser user, String company, String position, String location,
                                      String experience, String skill, Boolean matched, Integer minScore,
                                      Long careerSourceId, Instant dateFrom, Instant dateTo,
                                      int page, int size, boolean newestFirst) {
        validatePage(page, size);
        validateDateRange(dateFrom, dateTo);
        Specification<Job> specification = JobSpecifications.ownedBy(user.id())
                .and(activeJobs())
                .and(JobSpecifications.companyContains(clean(company)))
                .and(JobSpecifications.positionContains(clean(position)))
                .and(JobSpecifications.locationContains(clean(location)))
                .and(JobSpecifications.experienceContains(clean(experience)))
                .and(JobSpecifications.hasSkill(clean(skill)))
                .and(JobSpecifications.fromCareerSource(careerSourceId))
                .and(JobSpecifications.firstSeenAfter(dateFrom))
                .and(JobSpecifications.firstSeenBefore(dateTo))
                .and(JobSpecifications.matchCriteria(user.id(), matched, minScore));
        String sortField = newestFirst ? "firstSeenAt" : "lastSeenAt";
        Sort sort = Sort.by(Sort.Direction.DESC, sortField)
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return jobs.findAll(specification, PageRequest.of(page, size, sort)).map(JobServiceImpl::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse findById(AuthenticatedUser user, Long jobId) {
        Job job = jobs.findByIdAndCareerSourceUserId(jobId, user.id())
                .filter(Job::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        return toResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobMatchResponse> findMatches(AuthenticatedUser user, String company, String position,
                                              String location, String experience, Boolean matched, Integer minScore,
                                              Long careerSourceId, Long profileId, Instant dateFrom,
                                              Instant dateTo, int page, int size) {
        validatePage(page, size);
        validateDateRange(dateFrom, dateTo);
        Specification<JobMatch> specification = JobMatchSpecifications.ownedBy(user.id())
                .and(JobMatchSpecifications.matchedStatus(matched))
                .and(JobMatchSpecifications.scoreAtLeast(minScore))
                .and(JobMatchSpecifications.profileId(profileId))
                .and(JobMatchSpecifications.companyContains(clean(company)))
                .and(JobMatchSpecifications.positionContains(clean(position)))
                .and(JobMatchSpecifications.locationContains(clean(location)))
                .and(JobMatchSpecifications.experienceContains(clean(experience)))
                .and(JobMatchSpecifications.careerSourceId(careerSourceId))
                .and(JobMatchSpecifications.matchedAfter(dateFrom))
                .and(JobMatchSpecifications.matchedBefore(dateTo));
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "matchedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return matches.findAll(specification, pageable).map(JobServiceImpl::toMatchResponse);
    }

    private static Specification<Job> activeJobs() {
        return (root, query, builder) -> builder.isTrue(root.get("active"));
    }

    private static void validatePage(int page, int size) {
        if (page < 0) throw new BadRequestException("Page index must not be negative");
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private static void validateDateRange(Instant dateFrom, Instant dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("dateFrom must be less than or equal to dateTo");
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static JobResponse toResponse(Job job) {
        return new JobResponse(job.getId(), job.getCareerSource().getId(), job.getExternalJobId(),
                job.getTitle(), job.getNormalizedTitle(), job.getCompanyName(), job.getLocation(),
                job.getExperience(), job.getDescription(), job.getJobUrl(),
                job.getSkills().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                job.getContentHash(), job.getFirstSeenAt(), job.getLastSeenAt(), job.isActive());
    }

    private static JobMatchResponse toMatchResponse(JobMatch match) {
        return new JobMatchResponse(match.getId(), toResponse(match.getJob()),
                match.getSearchProfile().getId(), match.getSearchProfile().getName(),
                match.getMatchScore(), match.isMatched(), match.isPositionMatched(),
                match.isLocationMatched(), match.isExperienceMatched(), match.isSkillsMatched(),
                match.isKeywordMatched(), match.getMatchExplanation(), match.getRejectionReason(),
                match.getMatchedAt());
    }
}
