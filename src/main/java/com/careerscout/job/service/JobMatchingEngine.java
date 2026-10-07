package com.careerscout.job.service;

import com.careerscout.job.entity.Job;
import com.careerscout.profile.ExperienceLevel;
import com.careerscout.profile.SearchProfile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JobMatchingEngine {
    private static final Pattern EXPERIENCE_RANGE = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(?:-|to)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:\\+\\s*)?(?:years?|yrs?)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPERIENCE_MINIMUM = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*\\+\\s*(?:years?|yrs?)?|(?:minimum|min\\.?|at least)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:years?|yrs?)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");

    public JobMatchEvaluation evaluate(Job job, SearchProfile profile) {
        String title = normalize(job.getTitle());
        String position = normalize(profile.getPosition());
        boolean positionMatched = containsPhrase(title, position);
        boolean locationMatched = locationMatches(job.getLocation(), profile.getLocation());
        boolean experienceMatched = experienceMatches(job.getExperience(), profile.getExperienceLevel());
        Set<String> jobSkills = job.getSkills().stream().map(JobMatchingEngine::normalize)
                .collect(java.util.stream.Collectors.toSet());
        List<String> matchedSkills = profile.getSkills().stream()
                .filter(skill -> skillMatches(skill, jobSkills, job))
                .toList();
        boolean skillsMatched = profile.getSkills().isEmpty()
                || matchedSkills.size() == profile.getSkills().size();
        List<String> matchedKeywords = profile.getKeywords().stream()
                .filter(keyword -> containsPhrase(searchableText(job), normalize(keyword)))
                .toList();
        boolean keywordMatched = profile.getKeywords().isEmpty()
                || matchedKeywords.size() == profile.getKeywords().size();

        int score = score(profile, positionMatched, locationMatched, experienceMatched,
                matchedSkills.size(), matchedKeywords.size());
        List<String> explanation = new ArrayList<>();
        explanation.add(positionMatched ? "Position matched" : "Position did not match");
        explanation.add(locationMatched ? "Location matched" : "Location did not match");
        explanation.add(experienceMatched ? "Experience matched" : "Experience did not match");
        explanation.add(profile.getSkills().isEmpty() ? "No skills were required"
                : matchedSkills.size() + " of " + profile.getSkills().size() + " required skills matched");
        explanation.add(profile.getKeywords().isEmpty() ? "No keywords were required"
                : matchedKeywords.size() + " of " + profile.getKeywords().size() + " keywords matched");

        List<String> rejectionReasons = new ArrayList<>();
        if (!positionMatched) rejectionReasons.add("Position does not match the profile");
        if (!locationMatched) rejectionReasons.add("Location does not match the profile");
        if (!experienceMatched) {
            rejectionReasons.add(job.getExperience() == null || job.getExperience().isBlank()
                    ? "Job experience requirements could not be determined"
                    : "Experience does not match the profile");
        }
        if (!skillsMatched) rejectionReasons.add("One or more required skills are missing");
        if (!keywordMatched) rejectionReasons.add("One or more required keywords are missing");

        return new JobMatchEvaluation(score, rejectionReasons.isEmpty(), positionMatched, locationMatched,
                experienceMatched, skillsMatched, keywordMatched, String.join("; ", explanation),
                rejectionReasons.isEmpty() ? null : String.join("; ", rejectionReasons));
    }

    private static int score(SearchProfile profile, boolean positionMatched, boolean locationMatched,
                             boolean experienceMatched, int matchedSkillCount, int matchedKeywordCount) {
        int points = (positionMatched ? 30 : 0) + (locationMatched ? 25 : 0)
                + (experienceMatched ? 25 : 0);
        int maximum = 80;
        if (!profile.getSkills().isEmpty()) {
            points += Math.round(15.0f * matchedSkillCount / profile.getSkills().size());
            maximum += 15;
        }
        if (!profile.getKeywords().isEmpty()) {
            points += Math.round(5.0f * matchedKeywordCount / profile.getKeywords().size());
            maximum += 5;
        }
        return Math.round(100.0f * points / maximum);
    }

    private static boolean skillMatches(String skill, Set<String> jobSkills, Job job) {
        String normalizedSkill = normalize(skill);
        return jobSkills.contains(normalizedSkill)
                || containsPhrase(normalize(job.getTitle()), normalizedSkill)
                || containsPhrase(normalize(job.getDescription()), normalizedSkill);
    }

    private static String searchableText(Job job) {
        return normalize(String.join(" ", nullToEmpty(job.getTitle()), nullToEmpty(job.getDescription()),
                nullToEmpty(job.getLocation()), nullToEmpty(job.getExperience()),
                String.join(" ", job.getSkills())));
    }

    private static boolean locationMatches(String jobLocation, String profileLocation) {
        String job = normalize(jobLocation);
        String profile = normalize(profileLocation);
        if (job.isBlank() || profile.isBlank()) return false;
        if (profile.equals("any") || profile.equals("remote") && job.contains("remote")) return true;
        return containsPhrase(job, profile) || containsPhrase(profile, job);
    }

    private static boolean experienceMatches(String experience, ExperienceLevel required) {
        if (required == ExperienceLevel.ANY) return true;
        if (experience == null || experience.isBlank()) return false;
        String value = normalize(experience);
        if (value.contains("fresher") || value.contains("entry level") || value.contains("entry-level")
                || value.contains("no experience") || value.contains("new graduate")) {
            return required == ExperienceLevel.FRESHER || required == ExperienceLevel.ZERO_TO_ONE;
        }

        float minimum;
        float maximum;
        Matcher range = EXPERIENCE_RANGE.matcher(value);
        if (range.find()) {
            minimum = Float.parseFloat(range.group(1));
            maximum = Float.parseFloat(range.group(2));
        } else {
            Matcher minimumMatcher = EXPERIENCE_MINIMUM.matcher(value);
            if (minimumMatcher.find()) {
                String number = minimumMatcher.group(1) != null
                        ? minimumMatcher.group(1) : minimumMatcher.group(2);
                minimum = Float.parseFloat(number);
                maximum = Float.POSITIVE_INFINITY;
            } else {
                Matcher number = NUMBER.matcher(value);
                if (!number.find()) {
                    if (value.contains("senior") || value.contains("lead") || value.contains("principal")) {
                        return required == ExperienceLevel.FIVE_PLUS;
                    }
                    return false;
                }
                minimum = Float.parseFloat(number.group());
                maximum = Float.POSITIVE_INFINITY;
            }
        }

        float requiredMinimum;
        float requiredMaximum;
        switch (required) {
            case FRESHER, ZERO_TO_ONE -> {
                requiredMinimum = 0;
                requiredMaximum = 1;
            }
            case ONE_TO_THREE -> {
                requiredMinimum = 1;
                requiredMaximum = 3;
            }
            case THREE_TO_FIVE -> {
                requiredMinimum = 3;
                requiredMaximum = 5;
            }
            case FIVE_PLUS -> {
                requiredMinimum = 5;
                requiredMaximum = Float.POSITIVE_INFINITY;
            }
            case ANY -> {
                return true;
            }
            default -> throw new IllegalStateException("Unsupported experience level: " + required);
        }
        return minimum <= requiredMaximum && maximum >= requiredMinimum;
    }

    private static boolean containsPhrase(String text, String phrase) {
        if (text.isBlank() || phrase.isBlank()) return false;
        return (" " + text + " ").contains(" " + phrase + " ");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
