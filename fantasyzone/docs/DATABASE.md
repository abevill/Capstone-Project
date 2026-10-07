# Database Design

MySQL 8, eleven tables, created from the JPA entities by Hibernate (`ddl-auto=update`). Seed data is inserted at startup by
`DataInitializer` (roles, an admin account, 12 sample NFL players with week 1 stats for season 2025; the sample stat
numbers are made up for demos).

## ERD
```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ USER_ROLES : grants
    USERS ||--o{ LEAGUE_MEMBERS : joins
    LEAGUES ||--o{ LEAGUE_MEMBERS : has
    USERS ||--o{ FANTASY_TEAMS : owns
    LEAGUES ||--o{ FANTASY_TEAMS : contains
    FANTASY_TEAMS ||--o{ ROSTER_PLAYERS : "has spots"
    NFL_PLAYERS ||--o{ ROSTER_PLAYERS : fills
    LEAGUES ||--o{ MATCHUPS : schedules
    FANTASY_TEAMS ||--o{ MATCHUPS : "home team"
    FANTASY_TEAMS ||--o{ MATCHUPS : "away team"
    NFL_PLAYERS ||--o{ PLAYER_WEEKLY_STATS : records
    FANTASY_TEAMS ||--o{ ROSTER_TRANSACTIONS : logs
    NFL_PLAYERS ||--o{ ROSTER_TRANSACTIONS : involves
    USERS ||--o{ ROSTER_TRANSACTIONS : performs

    USERS {
        bigint id PK
        varchar email UK
        varchar display_name
        varchar password_hash
        boolean enabled
    }
    ROLES {
        bigint id PK
        varchar name UK
    }
    USER_ROLES {
        bigint user_id FK
        bigint role_id FK
    }
    LEAGUES {
        bigint id PK
        varchar name
        varchar join_code UK
        int season
        boolean rosters_locked
    }
    LEAGUE_MEMBERS {
        bigint id PK
        bigint user_id FK
        bigint league_id FK
        boolean commissioner
    }
    FANTASY_TEAMS {
        bigint id PK
        varchar name
        bigint league_id FK
        bigint owner_id FK
        int wins
        int losses
        int ties
        double points_for
    }
    NFL_PLAYERS {
        bigint id PK
        varchar external_id UK
        varchar name
        varchar position
        varchar nfl_team
    }
    ROSTER_PLAYERS {
        bigint id PK
        bigint fantasy_team_id FK
        bigint nfl_player_id FK
        varchar position_slot
    }
    MATCHUPS {
        bigint id PK
        bigint league_id FK
        int week
        bigint home_team_id FK
        bigint away_team_id FK
        double home_score
        double away_score
        boolean finalized
    }
    PLAYER_WEEKLY_STATS {
        bigint id PK
        bigint nfl_player_id FK
        int season
        int week
        int passing_yards
        int passing_touchdowns
        int interceptions
        int rushing_yards
        int rushing_touchdowns
        int receptions
        int receiving_yards
        int receiving_touchdowns
        int fumbles_lost
    }
    ROSTER_TRANSACTIONS {
        bigint id PK
        bigint fantasy_team_id FK
        bigint nfl_player_id FK
        bigint user_id FK
        varchar type
        varchar position_slot
        datetime created_at
    }
```

## Relationships
| Relationship | Type | How it is modeled |
|---|---|---|
| Users and roles | Many-to-many | `@ManyToMany` with join table `user_roles` |
| Users and leagues | Many-to-many | Bridge entity `league_members` (adds the `commissioner` flag) |
| Teams playing each other | Many-to-many over a season | `matchups` links a home team and an away team (both many-to-one to `fantasy_teams`) |
| League to matchups | One-to-many | `League.matchups` / `Matchup.league` |
| League to teams | One-to-many | `League.teams` / `FantasyTeam.league` |
| Fantasy team to roster spots | One-to-many / many-to-one | `FantasyTeam.roster` / `RosterPlayer.fantasyTeam` |
| NFL player to roster spots and stats | One-to-many | `RosterPlayer.nflPlayer`, `PlayerWeeklyStats.nflPlayer` |
| Fantasy team to roster history | One-to-many | `FantasyTeam.transactions` / `RosterTransaction.fantasyTeam`; every add and drop is logged with the player, slot, who made it, and when |

The three relationship types the course asks for (`@OneToMany`, `@ManyToOne`, `@ManyToMany`) are all used.
Cascading: users, leagues, and teams cascade to their dependent rows (`CascadeType.ALL`, `orphanRemoval`). All
`@ManyToOne` associations are `LAZY`; roles are `EAGER` because security needs them on every request.

## Constraints
| Table | Constraint |
|---|---|
| `users` | `email` UNIQUE, NOT NULL; `password_hash` NOT NULL (BCrypt) |
| `roles` | `name` UNIQUE |
| `leagues` | `join_code` UNIQUE |
| `league_members` | UNIQUE (`user_id`, `league_id`): a user joins a league once |
| `fantasy_teams` | UNIQUE (`league_id`, `name`); UNIQUE (`league_id`, `owner_id`): one team per user per league |
| `nfl_players` | `external_id` UNIQUE |
| `roster_players` | UNIQUE (`fantasy_team_id`, `nfl_player_id`) |
| `player_weekly_stats` | UNIQUE (`nfl_player_id`, `season`, `week`): no duplicate stat lines |
| `matchups` | A team cannot play itself (checked in `@PrePersist`/`@PreUpdate`) |
| `roster_transactions` | `type` is `ADD` or `DROP` (stored as text); append-only; deleted with its team |

Rules enforced in the service layer rather than the database: a player can be on only one team within a league, a
roster holds at most 15 players, and matchups need an even number of teams.

## Indexes
| Table | Index | Used for |
|---|---|---|
| `nfl_players` | `position` | Filtering players by position |
| `nfl_players` | `nfl_team` | Filtering players by NFL team |
| `matchups` | (`league_id`, `week`) | Listing a league's matchups for a week |
| `roster_transactions` | (`fantasy_team_id`, `created_at`) | Showing a team's recent roster history |
| All unique constraints | Implicit unique indexes | Login by email, join by code, duplicate checks |
| All foreign keys | Created by MySQL | Joins |

## Validation annotations
`@NotBlank`, `@Email`, `@Size` on `User`, `League`, and `FantasyTeam`; `@Valid` request records add `@Min`/`@Max`
(season) and password length (8 to 100).
