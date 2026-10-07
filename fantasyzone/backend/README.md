# FantasyZone Backend

Spring Boot 3 / Java 17 / MySQL / JWT. The external sports API supplies real-world data only;
this application owns leagues, rosters, matchups, scoring, and standings.

## Run
    export DB_URL=jdbc:mysql://localhost:3306/fantasyzone
    export DB_USERNAME=root
    export DB_PASSWORD=yourpassword
    export JWT_SECRET=<random string, 32+ characters>
    export ADMIN_PASSWORD=<admin password>
    mvn spring-boot:run

API docs (Swagger UI): http://localhost:8080/swagger-ui.html
Seeded admin: admin@fantasyzone.test (password from ADMIN_PASSWORD). Sample players and week-1 stats are
seeded for season 2025. Those stat numbers are made up for demos, not real NFL data.
Never commit secrets. Use environment variables locally and SSM Parameter Store on AWS.

## Public (no login)
GET /api/players/**, GET /api/leagues, GET /api/leagues/{id}, GET /api/leagues/{id}/standings

## Authenticated
Leagues (create, join, members), teams, rosters (add/drop), matchups. Commissioner-only: remove member,
lock rosters, generate matchups, calculate scores. Admin-only: /api/admin/**.

## Not built yet (updated)
External API client + stats sync, AWS deployment, CI/CD pipeline.

## Dashboard endpoints
GET /api/me/leagues, GET /api/me/teams (authenticated)

## Testing
    mvn test                 # unit tests + H2 integration tests; coverage report is created automatically
    mvn verify               # also checks the 70% line-coverage target (reports a warning if missed)
    open target/site/jacoco/index.html

- Unit tests (Mockito) cover every service, the JWT service, the rate limiter, and the user-details service.
- `ApiIntegrationTest` boots the real application with real JWT security and an in-memory H2 database, and walks the
  full workflow: register, login, league, teams, rosters, matchups, scoring, standings, admin, and the 401/403/404/409 errors.
- Entities, DTO records, and the application class are excluded from coverage (no logic to test).
- SonarQube: `mvn verify sonar:sonar -Dsonar.host.url=<url> -Dsonar.token=<token>` (coverage path is preconfigured).

## Postman
Import `postman/FantasyZone.postman_collection.json`, set the `adminPassword` variable to your ADMIN_PASSWORD,
and run the collection in order. About 62 requests cover every endpoint plus the error cases, each with assertions.
Command line / CI: `newman run postman/FantasyZone.postman_collection.json --env-var baseUrl=http://localhost:8080`

Note: the auth rate limit is 10 requests per minute per address. One run of the collection makes 8 auth calls, so wait a minute
before re-running it, or start the backend with SECURITY_RATE_LIMIT_MAX_REQUESTS=1000 while testing.
