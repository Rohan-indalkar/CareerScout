package com.careerscout.career.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CareerJobExtractorTest {
    private final CareerJobExtractor extractor = new CareerJobExtractor(new ObjectMapper());

    @Test
    void extractsJobPostingFromJsonLdGraphAndDeduplicatesTheMatchingLink() {
        String html = """
                <html><body>
                <script type="application/ld+json">
                {"@graph":[{"@type":"JobPosting","title":"Java Developer",
                "identifier":{"value":"REQ-42"},"url":"/jobs/req-42",
                "hiringOrganization":{"name":"Structured Corp"},
                "jobLocation":{"address":{"addressLocality":"Pune","addressRegion":"Maharashtra"}},
                "experienceRequirements":"0-1 years",
                "description":"<p>Build APIs</p>",
                "skills":["Java","Spring Boot","SQL"]}]}
                </script>
                <a href="/jobs/req-42">Java Developer</a>
                </body></html>
                """;

        List<ExtractedJob> jobs = extractor.extract(html, "Example Corp", "https://example.com/careers");

        assertEquals(1, jobs.size());
        ExtractedJob job = jobs.getFirst();
        assertEquals("REQ-42", job.externalJobId());
        assertEquals("java developer", job.normalizedTitle());
        assertEquals("Structured Corp", job.companyName());
        assertEquals("Pune; Maharashtra", job.location());
        assertEquals("0-1 years", job.experience());
        assertEquals("Build APIs", job.description());
        assertEquals("https://example.com/jobs/req-42", job.jobUrl());
        assertEquals(3, job.skills().size());
        assertEquals(64, job.contentHash().length());
    }

    @Test
    void extractsConservativeJobLinksWhenStructuredDataIsUnavailable() {
        List<ExtractedJob> jobs = extractor.extract("""
                <article><a href="/careers/software-engineer">Software Engineer</a>
                <span>Pune · Fresher</span></article>
                <a href="/about">About our company</a>
                """, "Example Corp", "https://example.com/careers");

        assertEquals(1, jobs.size());
        assertEquals("Software Engineer", jobs.getFirst().title());
        assertEquals("https://example.com/careers/software-engineer", jobs.getFirst().jobUrl());
        assertFalse(jobs.getFirst().contentHash().isBlank());
    }
}
