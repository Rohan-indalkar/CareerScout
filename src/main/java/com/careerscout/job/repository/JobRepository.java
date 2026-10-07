package com.careerscout.job.repository;

import com.careerscout.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    Optional<Job> findByIdAndCareerSourceUserId(Long id, Long userId);
    Optional<Job> findByCareerSourceIdAndExternalJobId(Long careerSourceId, String externalJobId);
    Optional<Job> findByCareerSourceIdAndJobUrl(Long careerSourceId, String jobUrl);
}
