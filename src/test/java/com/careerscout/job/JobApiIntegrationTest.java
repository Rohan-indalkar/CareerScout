package com.careerscout.job;

import com.careerscout.auth.repository.AuthSessionRepository;
import com.careerscout.auth.repository.RefreshTokenRepository;
import com.careerscout.career.crawler.CareerPageFetcher;
import com.careerscout.career.repository.CareerSourceRepository;
import com.careerscout.career.service.ScheduledCareerPageScanner;
import com.careerscout.common.exception.UpstreamServiceException;
import com.careerscout.job.entity.Job;
import com.careerscout.job.entity.JobMatch;
import com.careerscout.job.repository.JobMatchRepository;
import com.careerscout.job.repository.JobRepository;
import com.careerscout.profile.ExperienceLevel;
import com.careerscout.profile.SearchProfile;
import com.careerscout.profile.SearchProfileRepository;
import com.careerscout.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobApiIntegrationTest {
    private static final String PASSWORD = "StrongPass123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private AuthSessionRepository sessions;
    @Autowired private CareerSourceRepository careerSources;
    @Autowired private SearchProfileRepository searchProfiles;
    @Autowired private JobRepository jobs;
    @Autowired private JobMatchRepository matches;
    @Autowired private ScheduledCareerPageScanner scheduledScanner;
    @MockitoBean private CareerPageFetcher pageFetcher;

    @BeforeEach
    void cleanDatabase() {
        reset(pageFetcher);
        matches.deleteAll();
        jobs.deleteAll();
        searchProfiles.deleteAll();
        careerSources.deleteAll();
        refreshTokens.deleteAll();
        sessions.deleteAll();
        users.deleteAll();
    }

    @Test
    void scansCareerPageAndCreatesThenUpdatesDiscoveredJobsIdempotently() throws Exception {
        Fixture fixture = createFixture("scan-owner@example.com");
        String page = """
                <script type="application/ld+json">
                {"@graph":[
                {"@type":"JobPosting","title":"Java Developer","identifier":{"value":"REQ-1"},
                "url":"/jobs/req-1","jobLocation":{"address":{"addressLocality":"Pune"}},
                "experienceRequirements":"Fresher","description":"<p>Build APIs</p>",
                "skills":["Java","Spring Boot"]},
                {"@type":"JobPosting","title":"Senior Product Manager","identifier":{"value":"REQ-2"},
                "url":"/jobs/req-2","jobLocation":{"address":{"addressLocality":"Toronto"}},
                "experienceRequirements":"5+ years","description":"Lead product teams",
                "skills":["Python"]}
                ]}
                </script>
                """;
        when(pageFetcher.fetch("https://example.com/jobs")).thenReturn(page, page,
                page.replace("Build APIs", "Build reliable APIs"));

        mockMvc.perform(post("/api/v1/career-sources/" + fixture.source().getId() + "/scan")
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discoveredJobs").value(2))
                .andExpect(jsonPath("$.data.createdJobs").value(2))
                .andExpect(jsonPath("$.data.updatedJobs").value(0));

        mockMvc.perform(post("/api/v1/career-sources/" + fixture.source().getId() + "/scan")
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdJobs").value(0))
                .andExpect(jsonPath("$.data.unchangedJobs").value(2));

        mockMvc.perform(post("/api/v1/career-sources/" + fixture.source().getId() + "/scan")
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedJobs").value(1));

        mockMvc.perform(get("/api/v1/jobs/matches")
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].matchScore").value(100))
                .andExpect(jsonPath("$.data[0].matched").value(true))
                .andExpect(jsonPath("$.data[0].matchExplanation").value(
                        "Position matched; Location matched; Experience matched; 1 of 1 required skills matched; 1 of 1 keywords matched"));

        mockMvc.perform(get("/api/v1/jobs/matches")
                        .param("matched", "false")
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].matched").value(false))
                .andExpect(jsonPath("$.data[0].rejectionReason").value(
                        "Position does not match the profile; Location does not match the profile; Experience does not match the profile; One or more required skills are missing; One or more required keywords are missing"));

        mockMvc.perform(get("/api/v1/jobs").header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void scanDoesNotFetchCareerSourcesOwnedByAnotherUser() throws Exception {
        Fixture owner = createFixture("scan-owner-two@example.com");
        Fixture otherUser = createFixture("scan-other@example.com");

        mockMvc.perform(post("/api/v1/career-sources/" + owner.source().getId() + "/scan")
                        .header("Authorization", bearer(otherUser.token())))
                .andExpect(status().isNotFound());

        verifyNoInteractions(pageFetcher);
    }

    @Test
    void inactiveCareerSourcesCannotBeManuallyOrAutomaticallyScanned() throws Exception {
        Fixture fixture = createFixture("scan-inactive@example.com");
        fixture.source().setActive(false);
        careerSources.saveAndFlush(fixture.source());

        mockMvc.perform(post("/api/v1/career-sources/" + fixture.source().getId() + "/scan")
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isBadRequest());

        scheduledScanner.scanDueSources();
        verifyNoInteractions(pageFetcher);
    }

    @Test
    void failedFetchDoesNotReportSuccessOrAdvanceLastScannedTime() throws Exception {
        Fixture fixture = createFixture("scan-failure@example.com");
        when(pageFetcher.fetch("https://example.com/jobs"))
                .thenThrow(new UpstreamServiceException("Career page could not be fetched"));

        mockMvc.perform(post("/api/v1/career-sources/" + fixture.source().getId() + "/scan")
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false));

        assertNull(careerSources.findById(fixture.source().getId()).orElseThrow().getLastScannedAt());
        assertEquals(0L, jobs.count());
        verify(pageFetcher).fetch("https://example.com/jobs");
    }

    @Test
    void scheduledScanHonorsPerSourceIntervalsAndContinuesAfterOneSourceFails() throws Exception {
        String failedUrl = "https://first.example/careers";
        String successfulUrl = "https://second.example/careers";
        String notDueUrl = "https://third.example/careers";
        Fixture failed = createFixture("scheduled-fail@example.com", failedUrl);
        Fixture successful = createFixture("scheduled-success@example.com", successfulUrl);
        Fixture notDue = createFixture("scheduled-not-due@example.com", notDueUrl);
        notDue.source().markScanAttempted(Instant.now());
        careerSources.saveAndFlush(notDue.source());

        when(pageFetcher.fetch(failedUrl))
                .thenThrow(new UpstreamServiceException("Career page could not be fetched"));
        when(pageFetcher.fetch(successfulUrl)).thenReturn("""
                <script type="application/ld+json">
                {"@type":"JobPosting","title":"Java Developer","url":"/jobs/req-1",
                "jobLocation":{"address":{"addressLocality":"Pune"}},
                "experienceRequirements":"Fresher","skills":["Java"]}
                </script>
                """);

        scheduledScanner.scanDueSources();

        verify(pageFetcher).fetch(failedUrl);
        verify(pageFetcher).fetch(successfulUrl);
        verify(pageFetcher, never()).fetch(notDueUrl);
        assertEquals(1L, jobs.count());
        assertNull(careerSources.findById(failed.source().getId()).orElseThrow().getLastScannedAt());
        assertNotNull(careerSources.findById(failed.source().getId()).orElseThrow().getLastScanAttemptAt());
        assertNotNull(careerSources.findById(successful.source().getId()).orElseThrow().getLastScannedAt());
    }

    @Test
    void listsFiltersPaginatesAndOrdersOwnedActiveJobs() throws Exception {
        Fixture fixture = createFixture("jobs-owner@example.com");
        Job older = saveJob(fixture, "Java Developer", "Pune", Instant.parse("2025-01-01T10:00:00Z"));
        Job newer = saveJob(fixture, "Senior Java Developer", "Mumbai", Instant.parse("2025-02-01T10:00:00Z"));

        mockMvc.perform(get("/api/v1/jobs").header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(newer.getId()))
                .andExpect(jsonPath("$.pagination.totalElements").value(2));

        mockMvc.perform(get("/api/v1/jobs/new").header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(newer.getId()));

        mockMvc.perform(get("/api/v1/jobs")
                        .param("position", "Senior")
                        .param("location", "mumbai")
                        .param("skill", "java")
                        .param("dateFrom", "2025-01-15T00:00:00Z")
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(newer.getId()));

        mockMvc.perform(get("/api/v1/jobs")
                        .param("size", "1")
                        .param("page", "1")
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(older.getId()))
                .andExpect(jsonPath("$.pagination.totalPages").value(2));

        mockMvc.perform(get("/api/v1/jobs")
                        .param("size", "101")
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsMatchedJobsByProfileAndMinimumScore() throws Exception {
        Fixture fixture = createFixture("matches-owner@example.com");
        Job highScoreJob = saveJob(fixture, "Java Developer", "Pune", Instant.parse("2025-02-01T10:00:00Z"));
        Job lowScoreJob = saveJob(fixture, "Backend Engineer", "Pune", Instant.parse("2025-02-02T10:00:00Z"));
        matches.save(new JobMatch(highScoreJob, fixture.profile(), 96, true,
                true, true, true, true, true, "All criteria matched", null,
                Instant.parse("2025-02-01T10:00:00Z")));
        matches.save(new JobMatch(lowScoreJob, fixture.profile(), 72, true,
                true, true, true, false, true, "Most criteria matched", null,
                Instant.parse("2025-02-02T10:00:00Z")));

        mockMvc.perform(get("/api/v1/jobs/matches")
                        .param("minScore", "90")
                        .param("position", "developer")
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].job.id").value(highScoreJob.getId()))
                .andExpect(jsonPath("$.data[0].matchScore").value(96))
                .andExpect(jsonPath("$.data[0].matchExplanation").value("All criteria matched"));
    }

    @Test
    void jobDetailsAndQueriesAreScopedToTheAuthenticatedUser() throws Exception {
        Fixture owner = createFixture("job-owner@example.com");
        Job job = saveJob(owner, "Java Developer", "Pune", Instant.parse("2025-03-01T10:00:00Z"));
        Fixture anotherUser = createFixture("job-other@example.com");

        mockMvc.perform(get("/api/v1/jobs/" + job.getId())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Java Developer"))
                .andExpect(jsonPath("$.data.skills[0]").value("Java"));

        mockMvc.perform(get("/api/v1/jobs/" + job.getId())
                        .header("Authorization", bearer(anotherUser.token())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", bearer(anotherUser.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized());
    }

    private Fixture createFixture(String email) throws Exception {
        return createFixture(email, "https://example.com/jobs");
    }

    private Fixture createFixture(String email, String careerUrl) throws Exception {
        JsonNode registration = read(mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test User","email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn());
        String token = registration.path("data").path("accessToken").asText();

        JsonNode sourceResponse = read(mockMvc.perform(post("/api/v1/career-sources")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyName":"Example Corp","careerUrl":"%s","scanIntervalMinutes":60}
                                """.formatted(careerUrl)))
                .andExpect(status().isCreated())
                .andReturn());
        JsonNode profileResponse = read(mockMvc.perform(post("/api/v1/search-profiles")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Java Fresher","position":"Java Developer","location":"Pune","experienceLevel":"FRESHER","skills":["Java"],"keywords":["Spring"]}
                                """))
                .andExpect(status().isCreated())
                .andReturn());

        return new Fixture(token,
                careerSources.findById(sourceResponse.path("data").path("id").asLong()).orElseThrow(),
                searchProfiles.findById(profileResponse.path("data").path("id").asLong()).orElseThrow());
    }

    private Job saveJob(Fixture fixture, String title, String location, Instant firstSeenAt) {
        return jobs.save(new Job(fixture.source(), title.toLowerCase().replace(' ', '-'),
                title, title.toLowerCase(), location, "Fresher", "Job description",
                "https://example.com/jobs/" + title.toLowerCase().replace(' ', '-'),
                Set.of("Java", "Spring"), "hash-" + title.replace(' ', '-'), firstSeenAt));
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record Fixture(String token, com.careerscout.career.entity.CareerSource source,
                           SearchProfile profile) {
    }
}
