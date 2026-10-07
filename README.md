# CareerScout Backend

CareerScout is being developed incrementally as a modular Spring Boot backend. Completed phases include the project foundation, authentication and user management, career-source management, search-profile management, job listing/match APIs, manual career-page crawling/job extraction, profile-based job matching, and scheduled career-page scanning. Notifications are planned for a later phase.

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

Career sources require a bearer access token. URLs are restricted to HTTP/HTTPS, reject embedded credentials and local/private literal addresses, and are canonicalized for per-user duplicate detection. The accepted scan interval is 5–10,080 minutes. The scan interval is metadata only until scheduled scanning is added.

## Manual career-page scan

- `POST /api/v1/career-sources/{id}/scan` — fetch an owned, active career page, extract job postings, and insert/update job records

The response reports discovered, created, updated, and unchanged job counts. A successful scan updates the source's `lastScannedAt`. Duplicate postings are reconciled by the posting's structured identifier, then by canonical job URL; SHA-256 content hashes detect changed postings while preserving their original `firstSeenAt`. Each scan also evaluates discovered jobs against every active search profile belonging to the source owner and inserts or updates one profile-specific match record.

Extraction supports Schema.org `JobPosting` JSON-LD and a conservative fallback for links whose paths look like job/career/opening/position/role pages. The fallback cannot reliably infer structured location, experience, or skills. Client-rendered pages that require JavaScript are not rendered. Requests are bounded to an 8-second timeout and 2 MB response body; redirects are rejected, and resolved destinations must be public internet addresses.

## Search profile APIs

- `POST /api/v1/search-profiles` — create a search profile
- `GET /api/v1/search-profiles` — list only the authenticated user's profiles
- `GET /api/v1/search-profiles/{id}` — retrieve one owned profile
- `PUT /api/v1/search-profiles/{id}` — update position, location, experience, skills, and keywords
- `PATCH /api/v1/search-profiles/{id}/status` — activate/deactivate with `{"active": false}`
- `DELETE /api/v1/search-profiles/{id}` — delete an owned profile

The `experienceLevel` values are `FRESHER`, `ZERO_TO_ONE`, `ONE_TO_THREE`, `THREE_TO_FIVE`, `FIVE_PLUS`, and `ANY`. Profile names are unique per user after case/whitespace normalization. Profiles can be created independently of career sources; automatic matching is added in a later phase.

## Job APIs

All job endpoints require a bearer access token and return only jobs associated with career sources owned by the authenticated user. Job records and profile-specific match records are read-only through the API; they are populated by manual scans.

- `GET /api/v1/jobs` — list active jobs, newest last-seen first
- `GET /api/v1/jobs/new` — list active jobs, newest first-seen first
- `GET /api/v1/jobs/{id}` — retrieve one active, owned job
- `GET /api/v1/jobs/matches` — list successful matches with profile-specific explanations; pass `matched=false` to inspect rejected jobs and their reasons

The list endpoints accept optional filters: `company`, `position`, `location`, `experience`, `skill`, `careerSourceId`, `dateFrom`, and `dateTo`. The general jobs endpoints also accept `matched` and `minScore`; the matches endpoint accepts `matched` (defaults to `true`), `profileId`, and `minScore`. Dates use ISO-8601 timestamps. Pagination uses zero-based `page` (default `0`) and `size` (default `20`, maximum `100`).

Job responses include extracted job fields, skills, source ID, first/last-seen timestamps, and active status. Match responses include match score, individual criteria flags, explanation, rejection reason, and the associated search profile.

Matching uses case-insensitive phrase comparisons for position and location, experience-range overlap for experience levels, and checks required skills and keywords against extracted job data. All configured profile criteria must match for a result to be marked matched; `ANY` experience accepts unknown experience. The score weights are position 30%, location 25%, experience 25%, skills 15%, and keywords 5%; the score is normalized over criteria configured on a profile. A profile change is applied to its jobs on the next scan.

## Scheduled scanning

Active career sources are checked periodically and scanned when their `scanIntervalMinutes` has elapsed since the last scan attempt. The scheduler checks once per minute by default, starts 30 seconds after application startup, and isolates failures so one source cannot prevent other due sources from being checked. Failed attempts are logged and retried after the source's configured interval; successful scans update `lastScannedAt`.

Configure the scheduler with `CAREERSCOUT_SCAN_INITIAL_DELAY` and `CAREERSCOUT_SCAN_POLL_INTERVAL` as ISO-8601 durations (defaults `PT30S` and `PT1M`). Set a long duration or disable the scheduler in deployment configuration if scheduled scanning should not run in that environment.

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
