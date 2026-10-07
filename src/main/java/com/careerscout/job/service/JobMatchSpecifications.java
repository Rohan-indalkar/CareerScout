package com.careerscout.job.service;

import com.careerscout.job.entity.JobMatch;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;

public final class JobMatchSpecifications {
    private JobMatchSpecifications() {
    }

    public static Specification<JobMatch> ownedBy(Long userId) {
        return (root, query, builder) ->
                builder.equal(root.join("searchProfile").join("user").get("id"), userId);
    }

    public static Specification<JobMatch> isMatched(boolean matched) {
        return (root, query, builder) -> builder.equal(root.get("matched"), matched);
    }

    public static Specification<JobMatch> matchedStatus(Boolean matched) {
        return (root, query, builder) -> matched == null ? builder.conjunction()
                : builder.equal(root.get("matched"), matched);
    }

    public static Specification<JobMatch> scoreAtLeast(Integer score) {
        return (root, query, builder) -> score == null ? builder.conjunction()
                : builder.greaterThanOrEqualTo(root.get("matchScore"), score);
    }

    public static Specification<JobMatch> profileId(Long profileId) {
        return (root, query, builder) -> profileId == null ? builder.conjunction()
                : builder.equal(root.join("searchProfile").get("id"), profileId);
    }

    public static Specification<JobMatch> companyContains(String company) {
        return (root, query, builder) -> company == null ? builder.conjunction()
                : builder.like(builder.lower(root.join("job").get("companyName")), contains(company));
    }

    public static Specification<JobMatch> positionContains(String position) {
        return (root, query, builder) -> position == null ? builder.conjunction()
                : builder.like(builder.lower(root.join("job").get("title")), contains(position));
    }

    public static Specification<JobMatch> locationContains(String location) {
        return (root, query, builder) -> location == null ? builder.conjunction()
                : builder.like(builder.lower(root.join("job").get("location")), contains(location));
    }

    public static Specification<JobMatch> experienceContains(String experience) {
        return (root, query, builder) -> experience == null ? builder.conjunction()
                : builder.like(builder.lower(root.join("job").get("experience")), contains(experience));
    }

    public static Specification<JobMatch> careerSourceId(Long sourceId) {
        return (root, query, builder) -> sourceId == null ? builder.conjunction()
                : builder.equal(root.join("job").join("careerSource").get("id"), sourceId);
    }

    public static Specification<JobMatch> matchedAfter(Instant dateFrom) {
        return (root, query, builder) -> dateFrom == null ? builder.conjunction()
                : builder.greaterThanOrEqualTo(root.get("matchedAt"), dateFrom);
    }

    public static Specification<JobMatch> matchedBefore(Instant dateTo) {
        return (root, query, builder) -> dateTo == null ? builder.conjunction()
                : builder.lessThanOrEqualTo(root.get("matchedAt"), dateTo);
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
