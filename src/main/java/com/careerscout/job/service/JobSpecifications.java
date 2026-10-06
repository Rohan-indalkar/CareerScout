package com.careerscout.job.service;

import com.careerscout.job.entity.Job;
import com.careerscout.job.entity.JobMatch;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;

public final class JobSpecifications {
    private JobSpecifications() {
    }

    public static Specification<Job> ownedBy(Long userId) {
        return (root, query, builder) -> builder.equal(root.join("careerSource").join("user").get("id"), userId);
    }

    public static Specification<Job> companyContains(String company) {
        return (root, query, builder) -> company == null ? builder.conjunction()
                : builder.like(builder.lower(root.get("companyName")), contains(company));
    }

    public static Specification<Job> positionContains(String position) {
        return (root, query, builder) -> position == null ? builder.conjunction()
                : builder.like(builder.lower(root.get("title")), contains(position));
    }

    public static Specification<Job> locationContains(String location) {
        return (root, query, builder) -> location == null ? builder.conjunction()
                : builder.like(builder.lower(root.get("location")), contains(location));
    }

    public static Specification<Job> experienceContains(String experience) {
        return (root, query, builder) -> experience == null ? builder.conjunction()
                : builder.like(builder.lower(root.get("experience")), contains(experience));
    }

    public static Specification<Job> hasSkill(String skill) {
        return (root, query, builder) -> {
            if (skill == null) return builder.conjunction();
            query.distinct(true);
            return builder.equal(builder.lower(root.join("skills")), normalize(skill));
        };
    }

    public static Specification<Job> fromCareerSource(Long careerSourceId) {
        return (root, query, builder) -> careerSourceId == null ? builder.conjunction()
                : builder.equal(root.join("careerSource").get("id"), careerSourceId);
    }

    public static Specification<Job> firstSeenAfter(Instant dateFrom) {
        return (root, query, builder) -> dateFrom == null ? builder.conjunction()
                : builder.greaterThanOrEqualTo(root.get("firstSeenAt"), dateFrom);
    }

    public static Specification<Job> firstSeenBefore(Instant dateTo) {
        return (root, query, builder) -> dateTo == null ? builder.conjunction()
                : builder.lessThanOrEqualTo(root.get("firstSeenAt"), dateTo);
    }

    public static Specification<Job> matchCriteria(Long userId, Boolean matched, Integer minScore) {
        return (root, query, builder) -> {
            if (matched == null && minScore == null) return builder.conjunction();
            var subquery = query.subquery(Long.class);
            var match = subquery.from(JobMatch.class);
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.equal(match.get("job").get("id"), root.get("id")));
            predicates.add(builder.equal(match.join("searchProfile").join("user").get("id"), userId));
            if (matched != null) predicates.add(builder.equal(match.get("matched"), matched));
            if (minScore != null) predicates.add(builder.greaterThanOrEqualTo(match.get("matchScore"), minScore));
            subquery.select(match.get("id")).where(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
            return builder.exists(subquery);
        };
    }

    private static String contains(String value) {
        return "%" + normalize(value) + "%";
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
