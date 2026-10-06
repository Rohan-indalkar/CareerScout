package com.careerscout.job.service;

import com.careerscout.job.dto.JobMatchResponse;
import com.careerscout.job.dto.JobResponse;
import com.careerscout.security.AuthenticatedUser;
import org.springframework.data.domain.Page;

import java.time.Instant;

public interface JobService {
    Page<JobResponse> findJobs(AuthenticatedUser user, String company, String position, String location,
                               String experience, String skill, Boolean matched, Integer minScore,
                               Long careerSourceId, Instant dateFrom, Instant dateTo,
                               int page, int size, boolean newestFirst);

    JobResponse findById(AuthenticatedUser user, Long jobId);

    Page<JobMatchResponse> findMatches(AuthenticatedUser user, String company, String position,
                                       String location, String experience, Integer minScore,
                                       Long careerSourceId, Long profileId, Instant dateFrom,
                                       Instant dateTo, int page, int size);
}
