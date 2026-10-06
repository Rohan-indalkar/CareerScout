package com.careerscout.career.entity;

import com.careerscout.user.UserAccount;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "career_sources",
        uniqueConstraints = @UniqueConstraint(name = "uk_career_source_user_url",
                columnNames = {"user_id", "normalized_url"}),
        indexes = {
                @Index(name = "idx_career_sources_user", columnList = "user_id"),
                @Index(name = "idx_career_sources_active", columnList = "active")
        })
public class CareerSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(nullable = false, length = 2048)
    private String careerUrl;

    @Column(nullable = false, length = 2048)
    private String normalizedUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int scanIntervalMinutes;

    private Instant lastScannedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected CareerSource() {
    }

    public CareerSource(UserAccount user, String companyName, String careerUrl,
                        String normalizedUrl, int scanIntervalMinutes) {
        this.user = user;
        this.companyName = companyName;
        this.careerUrl = careerUrl;
        this.normalizedUrl = normalizedUrl;
        this.scanIntervalMinutes = scanIntervalMinutes;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String companyName, String careerUrl, String normalizedUrl, int scanIntervalMinutes) {
        this.companyName = companyName;
        this.careerUrl = careerUrl;
        this.normalizedUrl = normalizedUrl;
        this.scanIntervalMinutes = scanIntervalMinutes;
        this.updatedAt = Instant.now();
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public String getCompanyName() { return companyName; }
    public String getCareerUrl() { return careerUrl; }
    public String getNormalizedUrl() { return normalizedUrl; }
    public boolean isActive() { return active; }
    public int getScanIntervalMinutes() { return scanIntervalMinutes; }
    public Instant getLastScannedAt() { return lastScannedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
