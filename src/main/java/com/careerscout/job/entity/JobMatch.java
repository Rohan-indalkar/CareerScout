package com.careerscout.job.entity;

import com.careerscout.profile.SearchProfile;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "job_matches",
        uniqueConstraints = @UniqueConstraint(name = "uk_job_match_job_profile",
                columnNames = {"job_id", "search_profile_id"}),
        indexes = {
                @Index(name = "idx_job_matches_profile", columnList = "search_profile_id"),
                @Index(name = "idx_job_matches_score", columnList = "match_score")
        })
public class JobMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "search_profile_id", nullable = false)
    private SearchProfile searchProfile;

    @Column(nullable = false)
    private int matchScore;

    @Column(nullable = false)
    private boolean matched;

    @Column(nullable = false)
    private boolean positionMatched;

    @Column(nullable = false)
    private boolean locationMatched;

    @Column(nullable = false)
    private boolean experienceMatched;

    @Column(nullable = false)
    private boolean skillsMatched;

    @Column(nullable = false)
    private boolean keywordMatched;

    @Column(length = 2000)
    private String matchExplanation;

    @Column(length = 2000)
    private String rejectionReason;

    @Column(nullable = false)
    private Instant matchedAt;

    protected JobMatch() {
    }

    public JobMatch(Job job, SearchProfile searchProfile, int matchScore, boolean matched,
                    boolean positionMatched, boolean locationMatched, boolean experienceMatched,
                    boolean skillsMatched, boolean keywordMatched, String matchExplanation,
                    String rejectionReason, Instant matchedAt) {
        this.job = job;
        this.searchProfile = searchProfile;
        this.matchScore = matchScore;
        this.matched = matched;
        this.positionMatched = positionMatched;
        this.locationMatched = locationMatched;
        this.experienceMatched = experienceMatched;
        this.skillsMatched = skillsMatched;
        this.keywordMatched = keywordMatched;
        this.matchExplanation = matchExplanation;
        this.rejectionReason = rejectionReason;
        this.matchedAt = matchedAt == null ? Instant.now() : matchedAt;
    }

    public Long getId() { return id; }
    public Job getJob() { return job; }
    public SearchProfile getSearchProfile() { return searchProfile; }
    public int getMatchScore() { return matchScore; }
    public boolean isMatched() { return matched; }
    public boolean isPositionMatched() { return positionMatched; }
    public boolean isLocationMatched() { return locationMatched; }
    public boolean isExperienceMatched() { return experienceMatched; }
    public boolean isSkillsMatched() { return skillsMatched; }
    public boolean isKeywordMatched() { return keywordMatched; }
    public String getMatchExplanation() { return matchExplanation; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getMatchedAt() { return matchedAt; }
}
