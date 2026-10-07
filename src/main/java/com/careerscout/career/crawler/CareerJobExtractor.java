package com.careerscout.career.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class CareerJobExtractor {
    private static final Logger logger = LoggerFactory.getLogger(CareerJobExtractor.class);
    private static final int MAX_JOBS_PER_PAGE = 1_000;
    private static final Set<String> GENERIC_LINK_TEXT = Set.of(
            "apply", "apply now", "view job", "view role", "learn more", "read more", "details");

    private final ObjectMapper objectMapper;

    public CareerJobExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ExtractedJob> extract(String html, String companyName, String pageUrl) {
        Document document = Jsoup.parse(html, pageUrl);
        Map<String, ExtractedJob> discovered = new LinkedHashMap<>();

        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                JsonNode json = objectMapper.readTree(script.data());
                collectJobPostings(json, companyName, pageUrl, discovered);
            } catch (JsonProcessingException exception) {
                logger.warn("Ignoring malformed JSON-LD data while extracting jobs from {}", pageUrl, exception);
            }
        }

        collectJobLinks(document, companyName, pageUrl, discovered);
        return discovered.values().stream().limit(MAX_JOBS_PER_PAGE).toList();
    }

    private void collectJobPostings(JsonNode node, String companyName, String pageUrl,
                                   Map<String, ExtractedJob> discovered) {
        if (node == null) return;
        if (node.isArray()) {
            node.forEach(child -> collectJobPostings(child, companyName, pageUrl, discovered));
            return;
        }
        if (!node.isObject()) return;
        if (isJobPosting(node.path("@type"))) {
            ExtractedJob job = fromStructuredData(node, companyName, pageUrl);
            if (job != null) discovered.putIfAbsent(job.jobUrl(), job);
        }
        node.elements().forEachRemaining(child -> collectJobPostings(child, companyName, pageUrl, discovered));
    }

    private ExtractedJob fromStructuredData(JsonNode node, String companyName, String pageUrl) {
        String title = text(node.path("title"));
        String jobUrl = safeJobUrl(text(node.path("url")), pageUrl);
        if (title == null || jobUrl == null) return null;

        JsonNode organization = node.path("hiringOrganization");
        String externalId = text(node.path("identifier").path("value"));
        if (externalId == null) externalId = text(node.path("identifier"));
        String location = extractLocation(node.path("jobLocation"));
        if (location == null && text(node.path("jobLocationType")) != null
                && text(node.path("jobLocationType")).equalsIgnoreCase("TELECOMMUTE")) {
            location = "Remote";
        }
        return createJob(externalId, title, location,
                text(node.path("experienceRequirements")), text(node.path("description")),
                jobUrl, collectSkills(node.path("skills")), companyName, text(organization.path("name")));
    }

    private void collectJobLinks(Document document, String companyName, String pageUrl,
                                 Map<String, ExtractedJob> discovered) {
        for (Element anchor : document.select("a[href]")) {
            String title = cleanText(anchor.text());
            if (title == null || title.length() < 4 || GENERIC_LINK_TEXT.contains(title.toLowerCase(Locale.ROOT))) {
                continue;
            }
            String jobUrl = safeJobUrl(anchor.attr("href"), pageUrl);
            if (jobUrl == null || !looksLikeJobLink(jobUrl)) continue;

            Element card = anchor.parent();
            if (card == null) continue;
            String cardText = cleanText(card.text());
            String description = cardText == null || cardText.equals(title) ? null : cardText;
            String externalId = firstNonBlank(card.attr("data-job-id"), card.attr("data-id"),
                    idFromPath(jobUrl));
            ExtractedJob job = createJob(externalId, title, null, null, description,
                    jobUrl, Set.of(), companyName, null);
            discovered.putIfAbsent(jobUrl, job);
            if (discovered.size() >= MAX_JOBS_PER_PAGE) return;
        }
    }

    private ExtractedJob createJob(String externalId, String title, String location, String experience,
                                   String description, String jobUrl, Set<String> skills,
                                   String fallbackCompany, String structuredCompany) {
        String cleanTitle = limit(cleanText(title), 300);
        String cleanCompanyName = limit(firstNonBlank(structuredCompany, fallbackCompany), 150);
        String cleanLocation = limit(cleanText(location), 200);
        String cleanExperience = limit(cleanText(experience), 100);
        String cleanDescription = limit(cleanText(description), 20_000);
        String cleanExternalId = limit(cleanText(externalId), 255);
        Set<String> cleanSkills = new LinkedHashSet<>();
        skills.stream().map(CareerJobExtractor::cleanText).filter(value -> value != null)
                .map(value -> limit(value, 100)).forEach(cleanSkills::add);
        String normalizedTitle = cleanTitle.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        String hash = contentHash(cleanTitle, cleanCompanyName, cleanLocation, cleanExperience, cleanDescription,
                jobUrl, String.join(",", cleanSkills.stream().sorted().toList()));
        return new ExtractedJob(cleanExternalId, cleanTitle, normalizedTitle, cleanCompanyName,
                cleanLocation, cleanExperience, cleanDescription, jobUrl, Set.copyOf(cleanSkills), hash);
    }

    private String extractLocation(JsonNode locations) {
        List<String> found = new ArrayList<>();
        collectLocations(locations, found);
        return found.isEmpty() ? null : String.join("; ", new LinkedHashSet<>(found));
    }

    private void collectLocations(JsonNode node, List<String> found) {
        if (node == null || node.isMissingNode() || node.isNull()) return;
        if (node.isArray()) {
            node.forEach(child -> collectLocations(child, found));
        } else if (node.isObject()) {
            if (node.has("address")) {
                collectLocations(node.path("address"), found);
            } else {
                for (String field : List.of("streetAddress", "addressLocality", "addressRegion",
                        "postalCode", "addressCountry")) {
                    JsonNode part = node.path(field);
                    if (part.isObject()) part = part.path("name");
                    addLocationPart(found, text(part));
                }
            }
        } else if (node.isTextual()) {
            addLocationPart(found, node.asText());
        } else if (node.isValueNode()) {
            addLocationPart(found, node.asText());
        }
    }

    private static void addLocationPart(List<String> found, String part) {
        String clean = cleanText(part);
        if (clean != null) found.add(clean);
    }

    private Set<String> collectSkills(JsonNode skills) {
        Set<String> result = new LinkedHashSet<>();
        collectSkillValues(skills, result);
        return result;
    }

    private static void collectSkillValues(JsonNode node, Set<String> result) {
        if (node == null || node.isMissingNode() || node.isNull()) return;
        if (node.isArray()) {
            node.forEach(child -> collectSkillValues(child, result));
        } else if (node.isTextual()) {
            for (String skill : node.asText().split("[,;|]")) {
                String clean = cleanText(skill);
                if (clean != null) result.add(clean);
            }
        } else if (node.isObject()) {
            String name = firstNonBlank(text(node.path("name")), text(node.path("value")));
            if (name != null) result.add(name);
            else node.elements().forEachRemaining(child -> collectSkillValues(child, result));
        }
    }

    private static boolean isJobPosting(JsonNode type) {
        if (type.isArray()) {
            for (JsonNode item : type) {
                if (isJobPosting(item)) return true;
            }
            return false;
        }
        if (!type.isTextual()) return false;
        String name = type.asText();
        int separator = Math.max(name.lastIndexOf('/'), name.lastIndexOf(':'));
        return name.substring(separator + 1).equalsIgnoreCase("JobPosting");
    }

    private static boolean looksLikeJobLink(String url) {
        String path = URI.create(url).getPath();
        if (path == null) return false;
        String normalized = path.toLowerCase(Locale.ROOT);
        return normalized.contains("/job") || normalized.contains("/career")
                || normalized.contains("/opening") || normalized.contains("/position")
                || normalized.contains("/vacanc") || normalized.contains("/role");
    }

    private static String safeJobUrl(String link, String pageUrl) {
        if (link == null || link.isBlank()) return null;
        try {
            URI page = new URI(pageUrl);
            URI resolved = page.resolve(new URI(link.trim())).normalize();
            String scheme = resolved.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
                    || resolved.getHost() == null || resolved.getUserInfo() != null) return null;
            String result = resolved.toASCIIString();
            int fragmentIndex = result.indexOf('#');
            if (fragmentIndex >= 0) result = result.substring(0, fragmentIndex);
            return result.length() <= 2048 ? result : null;
        } catch (URISyntaxException | IllegalArgumentException exception) {
            return null;
        }
    }

    private static String idFromPath(String url) {
        String path = URI.create(url).getPath();
        if (path == null) return null;
        int lastSlash = path.lastIndexOf('/');
        String id = path.substring(lastSlash + 1);
        return id.isBlank() ? null : id;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (node.isTextual() || node.isValueNode()) return cleanText(node.asText());
        if (node.isObject()) {
            String value = firstNonBlank(text(node.path("name")), text(node.path("value")),
                    text(node.path("description")));
            if (value != null) return value;
            JsonNode months = node.path("monthsOfExperience");
            if (months.isNumber()) return months.asText() + " months";
            JsonNode years = node.path("yearsOfExperience");
            if (years.isNumber()) return years.asText() + " years";
        }
        return null;
    }

    private static String cleanText(String value) {
        if (value == null) return null;
        String clean = Jsoup.parse(value).text().replaceAll("\\s+", " ").trim();
        return clean.isBlank() ? null : clean;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String clean = cleanText(value);
            if (clean != null) return clean;
        }
        return null;
    }

    private static String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String contentHash(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                digest.update((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
