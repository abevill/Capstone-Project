# FantasyZone: Fantasy Sports League Manager

A full-stack fantasy football league manager. Users create or join leagues, build rosters from NFL players, follow weekly
head-to-head matchups, and watch standings update. Real-world player data comes from an external source, while
FantasyZone owns all league logic: membership, rosters, scheduling, fantasy scoring, records, and standings.

**Stack:** React 18 + TypeScript + Vite, Spring Boot 3 (Java 17), Spring Security + JWT, JPA/Hibernate, MySQL, JUnit 5 + Mockito, Postman.

## Features
- Register and log in (JWT, BCrypt), roles `USER` and `ADMIN`
- Public pages before sign-up: home, leagues, standings, NFL players
- Create and join leagues with a join code; commissioner tools per league
- One team per user per league; add and drop players with roster rules (no duplicates in a league, 15-player limit, roster lock)
- Roster history: every add and drop is logged and shown on the team page
- Round-robin matchup generation and score calculation from player stats
- Wins, losses, ties, and ranked standings
- Real NFL players and weekly stats synced from ESPN by an admin (or automatically)
- Admin page to enable or disable accounts
- Light and dark themes, responsive layout, keyboard and screen-reader friendly forms

## Project layout
```
backend/    Spring Boot API, tests, Postman collection
frontend/   React app
docs/       Architecture, database, API reference, decision records, diagrams
```

## Run it locally
**Prerequisites:** Java 17, Maven, Node 20, MySQL 8.

1. Create the database: `CREATE DATABASE fantasyzone;`
2. Start the API (tables and seed data are created automatically):
   ```
   cd backend
   export DB_URL=jdbc:mysql://localhost:3306/fantasyzone
   export DB_USERNAME=root
   export DB_PASSWORD=<your password>
   export JWT_SECRET=<random string, 32+ characters>
   export ADMIN_PASSWORD=<admin password>
   mvn spring-boot:run
   ```
3. Start the web app:
   ```
   cd frontend
   cp .env.example .env
   npm install
   npm run dev
   ```
4. Open http://localhost:5173. API docs: http://localhost:8080/swagger-ui.html

**Demo accounts:** `admin@fantasyzone.test` with your `ADMIN_PASSWORD`; register any other user from the sign-up page.
**Demo data:** by default 12 made-up sample NFL players with week 1 stats for season 2025; a league created with season 2025 uses them. For real 2026 data, see below.

### Use real 2026 NFL data (free, no API key)
FantasyZone reads ESPN's public NFL endpoints.
1. On a fresh database, start the API with `SEED_SAMPLE_PLAYERS=false` so the sample players are not created.
2. Log in as the admin and open the **Admin** page, then **Real NFL data**.
3. Click **Sync players** (about 30 seconds), then **Sync stats** for each week you want (or "Current week").
4. Create your league with season **2026**; matchups are scored from the synced stats.

Optional: set `SYNC_ENABLED=true` to refresh rosters daily and stats every 6 hours automatically.

### Try the main flow
1. Register two users in two browsers.
2. User A creates a league and a team; User B joins with the join code and creates a team.
3. Both add players from the Players page.
4. User A opens the league, clicks Generate matchups, then Calculate score on the Matchups tab.
5. Check the Standings tab.

## Tests
```
cd backend
mvn test      # unit + integration tests, coverage report in target/site/jacoco/index.html
mvn verify    # also checks the 70% line-coverage target
```
Import `backend/postman/FantasyZone.postman_collection.json` into Postman, set `adminPassword`, and run the collection.

## Documentation
- [Architecture and diagrams](docs/ARCHITECTURE.md)
- [Database design and ERD](docs/DATABASE.md)
- [API reference](docs/API.md)
- [Architecture decision records](docs/DECISIONS.md)

## Known limitations
These were in the original proposal but are not built in this release:
- Kickers and team defenses are not synced or scored (the fixed scoring formula covers QB, RB, WR, TE); only regular-season weeks are synced
- ESPN's endpoints are unofficial, so they can change without notice (the app keeps working from stored data)
- Commissioner score corrections and a member-approval step when joining
- Editing or deleting a league
- Trades, drafts, waivers, custom scoring, and playoffs (stretch goals)
- AWS deployment and CI/CD (not required for this project)
