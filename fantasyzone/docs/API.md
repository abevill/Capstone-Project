# API Reference

Base URL (local): `http://localhost:8080`. Interactive documentation is generated automatically:
`/swagger-ui.html` (Swagger UI) and `/v3/api-docs` (OpenAPI JSON). A Postman collection with 62 requests and assertions
is in `backend/postman/FantasyZone.postman_collection.json`.

## Conventions
- JSON requests and responses. Authenticated calls send `Authorization: Bearer <token>`.
- **Public** = no token needed. **Member** = must belong to the league (admins always pass). **Commissioner** = league
  commissioner (admins always pass). **Admin** = `ROLE_ADMIN`.
- List endpoints that return a page use `?page=0&size=20` (size is capped at 50) and respond with
  `{ content: [...], totalPages, totalElements, number }`.

### Error format
Every error uses the same shape:
```json
{ "timestamp": "2025-09-01T12:00:00Z", "status": 409, "error": "Conflict", "message": "Player is already on a team in this league" }
```
Validation errors (400) add the failing fields:
```json
{ "status": 400, "error": "Bad Request", "message": "Validation failed", "fieldErrors": { "email": "must be a well-formed email address" } }
```
| Status | Meaning |
|---|---|
| 400 | Validation failed or malformed body |
| 401 | Missing, invalid, or expired token; wrong credentials; disabled account |
| 403 | Logged in but not allowed (not a member, not the owner, not the commissioner, not an admin) |
| 404 | Resource not found |
| 409 | Conflicts with current state (duplicate, roster locked, already finalized, ...) |
| 429 | Too many auth requests (limit: 10 per minute per address) |
| 502 | The sports data provider (ESPN) failed during a sync |

## Authentication
| Method | Path | Access | Body | Success |
|---|---|---|---|---|
| POST | `/api/auth/register` | Public | `{ email, displayName (3-50), password (8-100) }` | 201 `AuthResponse` |
| POST | `/api/auth/login` | Public | `{ email, password }` | 200 `AuthResponse` |

```json
// AuthResponse
{ "userId": 3, "token": "eyJhbGciOi...", "email": "alice@example.com", "displayName": "Alice", "roles": ["ROLE_USER"] }
```
Errors: register 409 (email taken), 400 (validation); login 401 (bad credentials or disabled account).

## Leagues
| Method | Path | Access | Notes | Success |
|---|---|---|---|---|
| GET | `/api/leagues` | Public | Paged list | 200 page of `League` |
| GET | `/api/leagues/{leagueId}` | Public | | 200 `League` |
| GET | `/api/leagues/{leagueId}/standings` | Public | Ranked by wins, then points | 200 `[Standing]` |
| POST | `/api/leagues` | User | Body `{ name, season }`; caller becomes commissioner; response includes `joinCode` | 201 `League` |
| POST | `/api/leagues/join` | User | Body `{ joinCode }` | 200 `League` (404 bad code, 409 already a member) |
| GET | `/api/leagues/{leagueId}/members` | Member | | 200 `[Member]` |
| DELETE | `/api/leagues/{leagueId}/members/{userId}` | Commissioner | Cannot remove the commissioner or a member whose team already has matchups | 204 |
| PUT | `/api/leagues/{leagueId}/lock?locked=true` | Commissioner | Locks or unlocks roster moves | 200 `League` |

```json
// League
{ "id": 1, "name": "Friday Night League", "season": 2025, "rostersLocked": false, "memberCount": 4, "joinCode": "7F3A9C21" }
// joinCode only appears when you create the league or view it as its commissioner (GET /api/me/leagues)
// Member
{ "userId": 3, "displayName": "Alice", "commissioner": true }
// Standing
{ "rank": 1, "teamId": 7, "teamName": "Alice FC", "owner": "Alice", "wins": 2, "losses": 0, "ties": 0, "pointsFor": 154.3 }
```

## Teams
| Method | Path | Access | Notes | Success |
|---|---|---|---|---|
| GET | `/api/leagues/{leagueId}/teams` | Member | | 200 `[Team]` |
| POST | `/api/leagues/{leagueId}/teams` | Member | Body `{ name }`; one team per user per league; names unique per league | 201 `Team` |
| GET | `/api/teams/{teamId}` | Member of its league | | 200 `Team` |
| PUT | `/api/teams/{teamId}` | Team owner or admin | Body `{ name }` (rename) | 200 `Team` |

```json
// Team
{ "id": 7, "name": "Alice FC", "leagueId": 1, "owner": "Alice", "wins": 1, "losses": 0, "ties": 0, "pointsFor": 27.7 }
```

## Rosters
| Method | Path | Access | Notes | Success |
|---|---|---|---|---|
| GET | `/api/teams/{teamId}/roster` | Member of its league | | 200 `[RosterEntry]` |
| POST | `/api/teams/{teamId}/roster/players/{playerId}` | Team owner or admin | Body `{ positionSlot }` (for example QB, RB, WR, TE, FLEX, BENCH). 409 if the player is already on a team in the league, the roster has 15 players, or rosters are locked | 201 `RosterEntry` |
| DELETE | `/api/teams/{teamId}/roster/players/{playerId}` | Team owner or admin | 404 if not on the roster; 409 if rosters are locked | 204 |
| GET | `/api/teams/{teamId}/transactions` | Member of its league | Roster history: the 50 most recent adds and drops, newest first | 200 `[Transaction]` |

```json
// RosterEntry
{ "playerId": 1, "name": "Josh Allen", "position": "QB", "nflTeam": "BUF", "slot": "QB" }
// Transaction
{ "id": 4, "type": "ADD", "playerId": 1, "playerName": "Josh Allen", "position": "QB", "slot": "QB", "madeBy": "Alice", "at": "2025-09-01T12:00:00Z" }
```

## Matchups
| Method | Path | Access | Notes | Success |
|---|---|---|---|---|
| GET | `/api/leagues/{leagueId}/matchups?week=1` | Member | `week` optional | 200 `[Matchup]` |
| POST | `/api/leagues/{leagueId}/matchups/generate` | Commissioner | Round-robin schedule; needs an even number of teams (at least 2); 409 if already generated | 201 `[Matchup]` |
| PUT | `/api/matchups/{matchupId}/calculate` | Commissioner | Scores both teams from stored stats (bench excluded), updates records; 409 if already finalized | 200 `Matchup` |

```json
// Matchup
{ "id": 12, "week": 1, "homeTeamId": 7, "homeTeam": "Alice FC", "awayTeamId": 8, "awayTeam": "Bob United",
  "homeScore": 27.7, "awayScore": 0.0, "finalized": true }
```

## NFL players (public, read-only)
| Method | Path | Notes | Success |
|---|---|---|---|
| GET | `/api/players?name=&position=&team=&page=` | One filter at a time (name, then position, then team); sorted by name | 200 page of `Player` |
| GET | `/api/players/{playerId}` | | 200 `Player` |
| GET | `/api/players/{playerId}/stats` | Raw stats plus fantasy points calculated by the app | 200 `[StatLine]` |

```json
// Player
{ "id": 1, "externalId": "seed-1", "name": "Josh Allen", "position": "QB", "nflTeam": "BUF" }
// StatLine
{ "season": 2025, "week": 1, "fantasyPoints": 27.7,
  "stats": { "externalPlayerId": "seed-1", "season": 2025, "week": 1, "passingYards": 280, "passingTouchdowns": 2,
             "interceptions": 1, "rushingYards": 45, "rushingTouchdowns": 1, "receptions": 0,
             "receivingYards": 0, "receivingTouchdowns": 0, "fumblesLost": 0 } }
```

## Current user
| Method | Path | Access | Success |
|---|---|---|---|
| GET | `/api/me/leagues` | User | 200 `[League]` (with `joinCode` for leagues you commission) |
| GET | `/api/me/teams` | User | 200 `[Team]` |

## Administration
| Method | Path | Access | Notes | Success |
|---|---|---|---|---|
| GET | `/api/admin/users` | Admin | Paged | 200 page of `UserSummary` |
| PUT | `/api/admin/users/{userId}/status?enabled=false` | Admin | Cannot disable your own account; a disabled user's existing tokens stop working | 200 `UserSummary` |
| POST | `/api/admin/players/sync` | Admin | Pulls QB, RB, WR, and TE rosters for all teams from ESPN; adds new players and updates changed ones (about 30 seconds) | 200 `SyncResult` |
| POST | `/api/admin/stats/sync?season=2026&week=4` | Admin | `season` and `week` are optional (default: the current week). Pulls box scores of that regular-season week's finished games and adds or updates stat lines. Players not in the database are skipped, so sync players first. Safe to run repeatedly | 200 `SyncResult` (400 bad week, 502 ESPN failure) |

```json
// UserSummary
{ "id": 3, "email": "alice@example.com", "displayName": "Alice", "enabled": true, "roles": ["ROLE_USER"] }
// SyncResult
{ "playersCreated": 0, "playersUpdated": 3, "statLinesCreated": 212, "statLinesUpdated": 0, "skippedUnknownPlayers": 150 }
```

## Operations
| Method | Path | Access | Success |
|---|---|---|---|
| GET | `/actuator/health` | Public | 200 `{ "status": "UP" }` |

## Differences from the original proposal
The first proposal listed `PUT`/`DELETE /api/leagues/{leagueId}` (edit or delete a league), which are not built in this
release, and a separate `/api/players/search` (search is `GET /api/players?name=` instead). The stats sync
(`POST /api/admin/stats/sync`) is built and reads from ESPN.
