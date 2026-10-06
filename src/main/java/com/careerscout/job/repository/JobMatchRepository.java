package com.careerscout.job.repository;

import com.careerscout.job.entity.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobMatchRepository extends JpaRepository<JobMatch, Long>, JpaSpecificationExecutor<JobMatch> {
}
