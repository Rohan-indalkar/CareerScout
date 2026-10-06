package com.careerscout.profile;

import com.careerscout.user.UserAccount;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "search_profiles",
        uniqueConstraints = @UniqueConstraint(name = "uk_search_profile_owner_name",
                columnNames = {"user_id", "normalized_name"}),
        indexes = @Index(name = "idx_search_profiles_user_active", columnList = "user_id, active"))
public class SearchProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String normalizedName;

    @Column(nullable = false, length = 150)
    private String position;

    @Column(nullable = false, length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExperienceLevel experienceLevel;

    @ElementCollection
    @CollectionTable(name = "search_profile_skills",
            joinColumns = @JoinColumn(name = "search_profile_id"))
    @Column(name = "skill", nullable = false, length = 100)
    private Set<String> skills = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "search_profile_keywords",
            joinColumns = @JoinColumn(name = "search_profile_id"))
    @Column(name = "keyword", nullable = false, length = 100)
    private Set<String> keywords = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected SearchProfile() {
    }

    public SearchProfile(UserAccount user, String name, String normalizedName, String position,
                         String location, ExperienceLevel experienceLevel,
                         Set<String> skills, Set<String> keywords) {
        this.user = user;
        this.name = name;
        this.normalizedName = normalizedName;
        this.position = position;
        this.location = location;
        this.experienceLevel = experienceLevel;
        this.skills = new LinkedHashSet<>(skills);
        this.keywords = new LinkedHashSet<>(keywords);
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, String normalizedName, String position, String location,
                       ExperienceLevel experienceLevel, Set<String> skills, Set<String> keywords) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.position = position;
        this.location = location;
        this.experienceLevel = experienceLevel;
        this.skills = new LinkedHashSet<>(skills);
        this.keywords = new LinkedHashSet<>(keywords);
        this.updatedAt = Instant.now();
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public String getName() { return name; }
    public String getNormalizedName() { return normalizedName; }
    public String getPosition() { return position; }
    public String getLocation() { return location; }
    public ExperienceLevel getExperienceLevel() { return experienceLevel; }
    public Set<String> getSkills() { return Set.copyOf(skills); }
    public Set<String> getKeywords() { return Set.copyOf(keywords); }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
