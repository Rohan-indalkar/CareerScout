# CareerScout Backend

CareerScout is being developed incrementally as a modular Spring Boot backend. Completed phases include the project foundation, authentication and user management, career-source management, search-profile management, and job listing/match APIs. Crawling, automatic matching, scheduling, and notifications are planned backend phases.

## Requirements

- Java 21
- Maven 3.9+
- MySQL 8.4, or Docker with Docker Compose

## Run with Docker Compose

From the repository root, copy `.env.example` to `.env`, replace the example passwords, and run:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

The API listens on `http://localhost:8080`.

## Run locally

Start a MySQL server and set these environment variables before starting the application:

```text
DB_NAME=careerscout
DB_USERNAME=<your-local-database-user>
DB_PASSWORD=<your-local-database-password>
JWT_SECRET=<a-random-secret-of-at-least-32-bytes>
```

Then, from `backend/`:

```powershell
mvn spring-boot:run
```

The default profile is `dev`. Choose another profile with `SPRING_PROFILES_ACTIVE=test` or `SPRING_PROFILES_ACTIVE=prod`. Production requires `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET`; Hibernate schema generation is set to `validate` in that profile.

## Authentication APIs

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh` — rotates refresh tokens
- `POST /api/v1/auth/logout` — requires the active access token and its refresh token
- `GET /api/v1/users/me`
- `PUT /api/v1/users/me`
- `PATCH /api/v1/users/me/password`

All success and error responses use the common response envelope. Public registration always creates a `USER`; roles are never accepted from the request. Access tokens expire after 15 minutes by default and refresh tokens after 7 days. Refresh tokens are stored only as SHA-256 hashes. Logout revokes the session, including previously rotated access tokens. Password changes revoke all sessions.

## Career source APIs

- `POST /api/v1/career-sources` — create an owned career-page source
- `GET /api/v1/career-sources` — list only the authenticated user's sources
- `GET /api/v1/career-sources/{id}` — retrieve one owned source
- `PUT /api/v1/career-sources/{id}` — update company, URL, and scan interval
- `PATCH /api/v1/career-sources/{id}/status` — activate/deactivate with `{"active": false}`
- `DELETE /api/v1/career-sources/{id}` — delete an owned source

Career sources require a bearer access token. URLs are restricted to HTTP/HTTPS, reject embedded credentials and local/private literal addresses, and are canonicalized for per-user duplicate detection. The accepted scan interval is 5–10,080 minutes. Scanning is not part of this phase; manual and scheduled scan operations are added with the crawler/monitoring phases.

## Search profile APIs

- `POST /api/v1/search-profiles` — create a search profile
- `GET /api/v1/search-profiles` — list only the authenticated user's profiles
- `GET /api/v1/search-profiles/{id}` — retrieve one owned profile
- `PUT /api/v1/search-profiles/{id}` — update position, location, experience, skills, and keywords
- `PATCH /api/v1/search-profiles/{id}/status` — activate/deactivate with `{"active": false}`
- `DELETE /api/v1/search-profiles/{id}` — delete an owned profile

The `experienceLevel` values are `FRESHER`, `ZERO_TO_ONE`, `ONE_TO_THREE`, `THREE_TO_FIVE`, `FIVE_PLUS`, and `ANY`. Profile names are unique per user after case/whitespace normalization. Profiles can be created independently of career sources; automatic matching is added in a later phase.

## Job APIs

All job endpoints require a bearer access token and return only jobs associated with career sources owned by the authenticated user. Job records and profile-specific match records are read-only through the API; ingestion is added with the crawler phase.

- `GET /api/v1/jobs` — list active jobs, newest last-seen first
- `GET /api/v1/jobs/new` — list active jobs, newest first-seen first
- `GET /api/v1/jobs/{id}` — retrieve one active, owned job
- `GET /api/v1/jobs/matches` — list successful job matches with profile-specific explanations

The list endpoints accept optional filters: `company`, `position`, `location`, `experience`, `skill`, `careerSourceId`, `dateFrom`, and `dateTo`. The general jobs endpoints also accept `matched` and `minScore`; the matches endpoint accepts `profileId` and `minScore`. Dates use ISO-8601 timestamps. Pagination uses zero-based `page` (default `0`) and `size` (default `20`, maximum `100`).

Job responses include extracted job fields, skills, source ID, first/last-seen timestamps, and active status. Match responses include match score, individual criteria flags, explanation, rejection reason, and the associated search profile.

## Foundation endpoints

- `GET /api/v1/health` — standard API health response
- `GET /actuator/health` — Spring Boot health status
- `GET /swagger-ui.html` — interactive OpenAPI documentation
- `GET /v3/api-docs` — OpenAPI document

## Verify

From `backend/`, run:

```powershell
mvn test
```
