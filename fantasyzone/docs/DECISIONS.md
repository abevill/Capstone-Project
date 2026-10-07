# Architecture Decision Records

Each record states the context, the decision, and what it costs us.

## ADR-1: The external API supplies data only; FantasyZone runs the league
**Context.** A sports API can provide players and statistics, but it knows nothing about our leagues.
**Decision.** Store real-world data locally behind a `SportsDataService` interface. Leagues, rosters, matchups, scoring,
records, and standings are computed by our services.
**Consequences.** The app keeps working if the API is down, the scoring rules are ours to change, and the demo runs on
seed data. Cost: a sync job has to copy the data in (see ADR-14); until it runs, only sample seed data exists.

## ADR-2: Fantasy points are calculated, not stored
**Context.** Scoring rules may change, and stored points would go stale.
**Decision.** Store raw stats only. `FantasyScoringService` converts stats to points whenever a matchup is calculated or a
player's stats are viewed.
**Consequences.** One place to change the formula, easy to unit test. Cost: a little repeated computation. Matchup scores
are frozen into the `matchups` row when the commissioner finalizes them, so past results do not move.

## ADR-3: Users and leagues are linked by an explicit bridge entity
**Context.** Users and leagues are many-to-many, and we also need per-league data (is this person the commissioner?).
**Decision.** Model `league_members` as its own entity with `user`, `league`, and `commissioner`, instead of a bare
`@ManyToMany` join table. Users and roles, which carry no extra data, do use `@ManyToMany`.
**Consequences.** Commissioner status is naturally per league. Cost: slightly more code than an annotation-only join.

## ADR-4: Commissioner is league data, not a global role
**Context.** The assignment requires USER and ADMIN roles, and a person can run one league while playing in another.
**Decision.** Spring Security knows only `ROLE_USER` and `ROLE_ADMIN`. Commissioner rights are checked per league in
`LeagueAccess`; admins pass every league check.
**Consequences.** Clear, testable rules in one class. Cost: league permissions are enforced in the service layer rather
than with `@PreAuthorize` annotations.

## ADR-5: Stateless JWT authentication
**Context.** The React app and API are separate, and the API should scale without session storage.
**Decision.** Login returns a signed JWT (24 hours). A filter validates it on each request and reloads the user, so a
disabled account stops working immediately. Passwords are hashed with BCrypt. Secrets come from environment variables.
**Consequences.** No server sessions and simple CORS. Cost: the token lives in browser storage (acceptable for this
project; HttpOnly cookies would be the next hardening step), and there is no refresh-token flow.

## ADR-6: Public read access before registration
**Context.** Visitors should see what the app offers before creating an account.
**Decision.** Home, league list and detail, standings, and NFL player data are public. Rosters, teams, matchups, and every
write require login.
**Consequences.** Better first impression and a clear demonstration of route protection. Cost: standings are visible to
anyone who knows the league id.

## ADR-7: Layered architecture with thin controllers
**Context.** Graders and future developers need to find business rules quickly.
**Decision.** Controllers handle HTTP and validation only; services hold all rules and use constructor injection and
`@Transactional`; repositories hold queries; DTO records keep entities out of JSON.
**Consequences.** Services are easy to unit test with mocks. Cost: some mapping code between entities and DTOs.

## ADR-8: Round-robin scheduling with the circle method
**Context.** Matchups must be generated automatically and fairly.
**Decision.** Fix the first team, rotate the rest each week, and pair from the ends. With n teams (n even) this yields n-1
weeks in which every team plays every other team exactly once.
**Consequences.** Simple and provably complete (verified by a unit test). Cost: odd team counts are rejected rather than
given byes, and there is one pass through the schedule (no playoffs).

## ADR-9: One error format everywhere
**Context.** The front end needs predictable errors to show helpful messages.
**Decision.** A `@RestControllerAdvice` maps custom exceptions and framework errors to one JSON shape with the right status.
**Consequences.** The React `ApiError` class can show `message` and per-field `fieldErrors` uniformly. Internal errors are
logged and returned as a generic 500 message.

## ADR-10: Test against the real security stack with an in-memory database
**Context.** Mocked controller tests would not catch security mistakes.
**Decision.** Unit tests (Mockito) cover each service. `ApiIntegrationTest` boots the whole application with real JWT
security on H2 (MySQL mode) and walks the complete league workflow, including 401/403/404/409 cases.
**Consequences.** High confidence in authorization rules. Cost: the integration test is slower than unit tests, and H2 is
not byte-for-byte MySQL.

## ADR-11: TypeScript React with context and a small fetch hook
**Context.** The rubric asks for hooks, global state, and clean API integration.
**Decision.** `AuthContext` (with `useReducer`) and `ThemeContext` hold global state; a `useFetch` hook and an `Async`
component give every screen the same loading, error, and retry behavior; all HTTP lives in `services/api.ts`.
**Consequences.** Consistent UX and little duplicated code. Cost: no caching library, so some screens refetch on navigation.

## ADR-12: Skipping the AWS deployment
**Context.** The deployment phase (EC2, RDS, S3, CI/CD) is not required for this project.
**Decision.** The application is built to run locally with MySQL; configuration comes from environment variables so it can
be deployed later without code changes.
**Consequences.** No hosted URL, pipeline, or monitoring dashboard.

## ADR-13: Log every roster move in the same transaction as the move
**Context.** Managers dispute who dropped whom, and the proposal promised an audit trail.
**Decision.** `RosterService` writes a `roster_transactions` row (type, player, slot, acting user, time) inside the same
`@Transactional` method that adds or drops the player. Failed moves write nothing.
**Consequences.** The log can never disagree with the roster, and history is shown on each team page to league members.
Cost: one extra insert per move; the log is append-only, so there is no edit or undo.

## ADR-14: ESPN's public endpoints as the real data source
**Context.** The league needs current NFL players and weekly stats for the 2026 season, for free. ESPN publishes no official
API, but its site endpoints are widely used. Sleeper's open API does not cover weekly stats, BALLDONTLIE's free tier
excludes player stats, and nflverse (the stable alternative) publishes files rather than a live API.
**Decision.** `EspnSportsDataService` reads ESPN's team rosters, scoreboard, and per-game box scores (no API key).
An admin triggers the sync (or an optional scheduler does), and results are stored in `nfl_players` and
`player_weekly_stats` using upserts, so syncing is repeatable. The parser is tested against real responses captured on
2026-10-06 (week 4).
**Consequences.** Free and current within hours of a game ending, and the league keeps working from stored data if ESPN
is down. Costs: the endpoints are undocumented and may change or be restricted without notice, so failures return a
clear 502 and a different provider can replace the class behind `SportsDataService`. Only regular-season weeks and the
positions the scoring formula covers (QB, RB, WR, TE) are synced; kickers and team defenses are not scored.
