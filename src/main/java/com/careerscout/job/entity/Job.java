package com.careerscout.job.entity;

import com.careerscout.career.entity.CareerSource;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "jobs",
        uniqueConstraints = @UniqueConstraint(name = "uk_jobs_source_external_id",
                columnNames = {"career_source_id", "external_job_id"}),
        indexes = {
                @Index(name = "idx_jobs_normalized_title", columnList = "normalized_title"),
                @Index(name = "idx_jobs_location", columnList = "location"),
                @Index(name = "idx_jobs_last_seen", columnList = "last_seen_at"),
                @Index(name = "idx_jobs_source", columnList = "career_source_id"),
                @Index(name = "idx_jobs_content_hash", columnList = "content_hash")
        })
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "career_source_id", nullable = false)
    private CareerSource careerSource;

    @Column(length = 255)
    private String externalJobId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 300)
    private String normalizedTitle;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(length = 200)
    private String location;

    @Column(length = 100)
    private String experience;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 2048)
    private String jobUrl;

    @ElementCollection
    @CollectionTable(name = "job_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill", nullable = false, length = 100)
    private Set<String> skills = new LinkedHashSet<>();

    @Column(length = 64)
    private String contentHash;

    @Column(nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(nullable = false)
    private Instant lastSeenAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Job() {
    }

    public Job(CareerSource careerSource, String externalJobId, String title, String normalizedTitle,
               String location, String experience, String description, String jobUrl,
               Set<String> skills, String contentHash, Instant firstSeenAt) {
        this.careerSource = careerSource;
        this.externalJobId = externalJobId;
        this.title = title;
        this.normalizedTitle = normalizedTitle;
        this.companyName = careerSource.getCompanyName();
        this.location = location;
        this.experience = experience;
        this.description = description;
        this.jobUrl = jobUrl;
        this.skills = new LinkedHashSet<>(skills);
        this.contentHash = contentHash;
        this.firstSeenAt = firstSeenAt == null ? Instant.now() : firstSeenAt;
        this.lastSeenAt = this.firstSeenAt;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() { return id; }
    public CareerSource getCareerSource() { return careerSource; }
    public String getExternalJobId() { return externalJobId; }
    public String getTitle() { return title; }
    public String getNormalizedTitle() { return normalizedTitle; }
    public String getCompanyName() { return companyName; }
    public String getLocation() { return location; }
    public String getExperience() { return experience; }
    public String getDescription() { return description; }
    public String getJobUrl() { return jobUrl; }
    public Set<String> getSkills() { return Set.copyOf(skills); }
    public String getContentHash() { return contentHash; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
