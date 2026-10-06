package com.careerscout.auth;

import com.careerscout.career.repository.CareerSourceRepository;
import com.careerscout.auth.repository.RefreshTokenRepository;
import com.careerscout.auth.repository.AuthSessionRepository;
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
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthIntegrationTest.AdminOnlyTestController.class)
class AuthIntegrationTest {
    private static final String EMAIL = "rohan@example.com";
    private static final String PASSWORD = "StrongPass123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private AuthSessionRepository sessions;
    @Autowired private CareerSourceRepository careerSources;
    @Autowired private SearchProfileRepository searchProfiles;

    @BeforeEach
    void cleanDatabase() {
        searchProfiles.deleteAll();
        careerSources.deleteAll();
        refreshTokens.deleteAll();
        sessions.deleteAll();
        users.deleteAll();
    }

    @Test
    void registrationReturnsTokenPairAndNeverReturnsPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value(EMAIL))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }

    @Test
    void duplicateEmailIsRejectedAndInvalidInputIsValidated() throws Exception {
        register();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                        {"name":"Test","email":"not-an-email","password":"12345678"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void invalidCredentialsDoNotRevealWhetherTheEmailExists() throws Exception {
        register();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", EMAIL, "password", "WrongPassword123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void userRoleCannotAccessAdminOnlyOperation() throws Exception {
        String accessToken = register().path("data").path("accessToken").asText();
        mockMvc.perform(get("/api/v1/test/admin")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loginAndAuthenticatedProfileWorkWhileAnonymousRequestIsRejected() throws Exception {
        register();
        String accessToken = login(PASSWORD).path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.name").value("Rohan"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void profileCanBeUpdatedAndEmailIsNormalized() throws Exception {
        String accessToken = register().path("data").path("accessToken").asText();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rohan K","email":"ROHAN@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Rohan K"))
                .andExpect(jsonPath("$.data.email").value(EMAIL));
    }

    @Test
    void refreshRotatesTokenAndLogoutRevokesTheCurrentSession() throws Exception {
        JsonNode tokens = register();
        String accessToken = tokens.path("data").path("accessToken").asText();
        String refreshToken = tokens.path("data").path("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        JsonNode rotated = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + rotated.path("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "refreshToken", rotated.path("refreshToken").asText()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("logged_out"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + rotated.path("accessToken").asText()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "refreshToken", rotated.path("refreshToken").asText()))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutCannotRevokeAnotherLoginSession() throws Exception {
        JsonNode firstLogin = register().path("data");
        JsonNode secondLogin = login(PASSWORD).path("data");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + secondLogin.path("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "refreshToken", firstLogin.path("refreshToken").asText()))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + secondLogin.path("accessToken").asText()))
                .andExpect(status().isOk());
    }

    @Test
    void passwordChangeRequiresCurrentPasswordAndRevokesRefreshTokens() throws Exception {
        JsonNode tokens = register();
        String accessToken = tokens.path("data").path("accessToken").asText();
        String refreshToken = tokens.path("data").path("refreshToken").asText();

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"incorrect","newPassword":"AnotherStrong123!"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"StrongPass123!","newPassword":"AnotherStrong123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("updated"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        org.junit.jupiter.api.Assertions.assertTrue(
                login("AnotherStrong123!").path("data").path("accessToken").asText().length() > 20);
    }

    @Test
    void careerSourceCrudAndStatusAreAvailableToOwner() throws Exception {
        String accessToken = register().path("data").path("accessToken").asText();
        String sourceId = createCareerSource(accessToken, "Example Co",
                "https://example.com/careers/", 60).path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/career-sources").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].careerUrl").value("https://example.com/careers"));

        mockMvc.perform(get("/api/v1/career-sources/" + sourceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("Example Co"));

        mockMvc.perform(put("/api/v1/career-sources/" + sourceId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(careerSourceBody("Example Corporation", "https://example.com/jobs", 120)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("Example Corporation"))
                .andExpect(jsonPath("$.data.scanIntervalMinutes").value(120));

        mockMvc.perform(patch("/api/v1/career-sources/" + sourceId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(delete("/api/v1/career-sources/" + sourceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("deleted"));

        mockMvc.perform(get("/api/v1/career-sources/" + sourceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void careerSourceRejectsUnsafeInvalidAndDuplicateUrls() throws Exception {
        String accessToken = register().path("data").path("accessToken").asText();

        mockMvc.perform(post("/api/v1/career-sources")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(careerSourceBody("Bad", "file:///etc/passwd", 60)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/career-sources")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(careerSourceBody("Bad", "http://127.0.0.1/careers", 60)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/career-sources")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(careerSourceBody("Bad", "https://user:pass@example.com/careers", 60)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/career-sources")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(careerSourceBody("Bad", "https://example.com/careers", 4)))
                .andExpect(status().isBadRequest());

        createCareerSource(accessToken, "Example", "https://EXAMPLE.com/careers/", 60);
        mockMvc.perform(post("/api/v1/career-sources")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(careerSourceBody("Duplicate", "https://example.com/careers", 60)))
                .andExpect(status().isConflict());
    }

    @Test
    void usersCannotReadUpdateOrDeleteAnotherUsersCareerSource() throws Exception {
        String ownerToken = register().path("data").path("accessToken").asText();
        String sourceId = createCareerSource(ownerToken, "Owner", "https://example.com/careers", 60)
                .path("data").path("id").asText();
        String otherToken = registerWith("another@example.com").path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/career-sources/" + sourceId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/career-sources/" + sourceId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(careerSourceBody("Other", "https://other.example/jobs", 60)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/career-sources/" + sourceId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/career-sources").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void searchProfilesSupportCrudStatusAndMultipleProfiles() throws Exception {
        String accessToken = register().path("data").path("accessToken").asText();
        String firstId = createSearchProfile(accessToken, searchProfileBody(
                "Java Fresher Pune", "Java Developer", "Pune", "FRESHER",
                "[\"Java\",\"Spring Boot\",\"SQL\"]", "[\"Backend Developer\"]"))
                .path("data").path("id").asText();
        createSearchProfile(accessToken, searchProfileBody(
                "Remote Engineer", "Software Engineer", "Remote", "ANY",
                "[]", "[\"Remote\"]"));

        mockMvc.perform(get("/api/v1/search-profiles").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].skills.length()").value(3));

        mockMvc.perform(get("/api/v1/search-profiles/" + firstId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Java Fresher Pune"))
                .andExpect(jsonPath("$.data.experienceLevel").value("FRESHER"));

        mockMvc.perform(put("/api/v1/search-profiles/" + firstId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchProfileBody("Java Fresher Pune", "Backend Engineer", "Mumbai",
                                "ZERO_TO_ONE", "[\"Java\"]", "[\"Spring\"]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.position").value("Backend Engineer"))
                .andExpect(jsonPath("$.data.location").value("Mumbai"))
                .andExpect(jsonPath("$.data.experienceLevel").value("ZERO_TO_ONE"))
                .andExpect(jsonPath("$.data.skills.length()").value(1));

        mockMvc.perform(patch("/api/v1/search-profiles/" + firstId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(delete("/api/v1/search-profiles/" + firstId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("deleted"));

        mockMvc.perform(get("/api/v1/search-profiles/" + firstId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchProfilesValidateInputsAndRejectDuplicateNames() throws Exception {
        String accessToken = register().path("data").path("accessToken").asText();

        mockMvc.perform(post("/api/v1/search-profiles")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchProfileBody("", "", "", "FRESHER", "[]", "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.position").exists())
                .andExpect(jsonPath("$.errors.location").exists());

        createSearchProfile(accessToken, searchProfileBody(
                "Java Fresher Pune", "Java Developer", "Pune", "FRESHER", "[]", "[]"));

        mockMvc.perform(post("/api/v1/search-profiles")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchProfileBody("  JAVA   FRESHER PUNE ", "Engineer", "Pune",
                                "FRESHER", "[]", "[]")))
                .andExpect(status().isConflict());
    }

    @Test
    void usersCannotReadUpdateOrDeleteAnotherUsersSearchProfile() throws Exception {
        String ownerToken = register().path("data").path("accessToken").asText();
        String profileId = createSearchProfile(ownerToken, searchProfileBody(
                "Owner Profile", "Java Developer", "Pune", "FRESHER", "[]", "[]"))
                .path("data").path("id").asText();
        String otherToken = registerWith("another@example.com").path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/search-profiles/" + profileId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/search-profiles/" + profileId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchProfileBody("Other", "Engineer", "Remote", "ANY", "[]", "[]")))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/search-profiles/" + profileId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/search-profiles/" + profileId + "/status")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/search-profiles").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private JsonNode register() throws Exception {
        return registerWith(EMAIL);
    }

    private JsonNode registerWith(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rohan","email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode login(String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", EMAIL, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String registerBody() {
        return """
                {"name":"Rohan","email":"%s","password":"%s"}
                """.formatted(EMAIL, PASSWORD);
    }

    private JsonNode createCareerSource(String accessToken, String company, String url, int scanInterval)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/career-sources")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(careerSourceBody(company, url, scanInterval)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String careerSourceBody(String company, String url, int scanInterval) {
        return """
                {"companyName":"%s","careerUrl":"%s","scanIntervalMinutes":%d}
                """.formatted(company, url, scanInterval);
    }

    private JsonNode createSearchProfile(String accessToken, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/search-profiles")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String searchProfileBody(String name, String position, String location, String experienceLevel,
                                     String skills, String keywords) {
        return """
                {"name":"%s","position":"%s","location":"%s","experienceLevel":"%s",
                 "skills":%s,"keywords":%s}
                """.formatted(name, position, location, experienceLevel, skills, keywords);
    }

    @org.springframework.web.bind.annotation.RestController
    static class AdminOnlyTestController {
        @org.springframework.web.bind.annotation.GetMapping("/api/v1/test/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String adminOnly() {
            return "admin";
        }
    }
}
