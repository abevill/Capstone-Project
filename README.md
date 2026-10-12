FantasyZone README.md
FantasyZone

FantasyZone is a full-stack fantasy football web application that allows users to create leagues, build fantasy teams, manage rosters, track player performance, and compete using real NFL statistics. The application uses ESPN NFL data as a source of player information and statistics while FantasyZone independently manages league operations, roster management, scoring, standings, and team ownership.

Features
User Management
User registration and authentication
JWT-based security
Role-based access control
Admin and user roles
League Management
Create fantasy leagues
Join leagues using unique join codes
View league standings
Manage league membership
Team Management
Create fantasy teams
Manage team rosters
View team performance
Track wins, losses, and ties
NFL Data Integration
Synchronize player information from ESPN
Synchronize weekly NFL statistics
Display player profiles and weekly stats
Calculate fantasy points from real-world performance
Roster Management
Add and remove players
Position-based roster assignments
Bench support
Fantasy football lineup management
AI Assistant
Integrated LLM-powered fantasy football assistant
Answers questions about players, fantasy football concepts, and roster decisions
Accessible directly inside FantasyZone
Technology Stack
Frontend
React
TypeScript
Vite
CSS
Backend
Spring Boot
Spring Security
Spring Data JPA
Hibernate
Database
MySQL
External Data Source
ESPN NFL API
AI Integration
GEMINI API
Installation & Setup
Prerequisites

Install the following software:

Java 17+
Node.js 18+
MySQL 8+
Maven
Clone Repository
git clone https://github.com/your-repository/fantasyzone.git
cd fantasyzone

Database Setup

Create a database:

CREATE DATABASE fantasyzone;

Backend Configuration

Edit:

backend/src/main/resources/application.properties


Configure:

spring.datasource.url=jdbc:mysql://localhost:3306/fantasyzone
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD

spring.jpa.hibernate.ddl-auto=update

Start Backend

Navigate to:

backend


Run:

mvn clean install
mvn spring-boot:run


Backend launches on:

http://localhost:8080

Start Frontend

Navigate to:

frontend


Install dependencies:

npm install


Start application:

npm run dev


Frontend launches on:

http://localhost:5173

Using FantasyZone
Administrator Workflow
Login as administrator
Navigate to Admin page
Sync NFL Players
Sync Weekly Statistics
Verify player imports
User Workflow
Register account
Login
Create league
Create fantasy team
Browse NFL players
Draft players to roster
Monitor player performance
View standings
API Documentation
Authentication
Register
POST /api/auth/register


Request:

{
  "email": "user@email.com",
  "displayName": "User",
  "password": "Password123!"
}

Login
POST /api/auth/login


Request:

{
  "email": "user@email.com",
  "password": "Password123!"
}


Response:

{
  "token": "jwt-token"
}

Leagues
Create League
POST /api/leagues

List Leagues
GET /api/leagues

Join League
POST /api/leagues/join

Teams
Create Team
POST /api/leagues/{leagueId}/teams

View Team
GET /api/teams/{teamId}

View Team Roster
GET /api/teams/{teamId}/roster

Players
List Players
GET /api/players

Player Details
GET /api/players/{playerId}

Player Weekly Statistics
GET /api/players/{playerId}/stats

Admin
Sync Players
POST /api/admin/sync/players

Sync Stats
POST /api/admin/sync/stats

Database Documentation
Main Entities
Users

Stores registered users.

id
email
display_name
password_hash
enabled

Roles

Stores security roles.

id
name

Leagues

Stores fantasy leagues.

id
name
season
join_code
rosters_locked

League Members

Connects users and leagues.

league_id
user_id
role

Fantasy Teams

Stores team information.

id
name
league_id
owner_id
wins
losses
ties
points_for
points_against

NFL Players

Stores NFL player information.

id
external_id
name
position
nfl_team

Player Weekly Stats

Stores weekly NFL statistics.

id
player_id
season
week
passing_yards
rushing_yards
receiving_yards
touchdowns
fantasy_points

Roster Players

Connects fantasy teams to NFL players.

id
fantasy_team_id
player_id
slot

Entity Relationship Diagram (ERD)
Users
  |
  | 1:M
  |
LeagueMembers
  |
  | M:1
  |
Leagues
  |
  | 1:M
  |
FantasyTeams
  |
  | 1:M
  |
RosterPlayers
  |
  | M:1
  |
NFLPlayers
  |
  | 1:M
  |
PlayerWeeklyStats

Deployment Guide
Build Backend
mvn clean package


Generated artifact:

target/fantasyzone.jar


Run:

java -jar fantasyzone.jar

Build Frontend
npm run build


Generated files:

dist/


Deploy contents to:

Netlify
Vercel
Azure Static Web Apps
AWS S3
Production Database

Create production schema:

CREATE DATABASE fantasyzone;


Update production credentials:

spring.datasource.url=
spring.datasource.username=
spring.datasource.password=

Architecture Decision Records (ADR)
ADR-001: React + Spring Boot
Decision

Use React for the frontend and Spring Boot for the backend.

Reason

This architecture provides a clear separation between presentation, business logic, and persistence layers.

ADR-002: MySQL Database
Decision

Use MySQL for persistent storage.

Reason

Provides reliable relational storage and integrates well with Spring Data JPA.

ADR-003: ESPN Data Provider
Decision

Use ESPN only as a source of player data and statistics.

Reason

FantasyZone controls league management, roster management, standings, and scoring independently rather than relying on ESPN fantasy league services.

ADR-004: JWT Authentication
Decision

Use JSON Web Tokens for authentication.

Reason

Provides secure stateless authentication between the React frontend and Spring Boot backend.

ADR-005: AI Fantasy Assistant
Decision

Integrate an LLM-powered assistant.

Reason

Provides users with fantasy football insights while demonstrating modern AI integration techniques.

Future Enhancements
Automatic roster position assignment
Position validation
Trade system
Waiver wire management
Live scoring updates
League customization settings
Draft room functionality
Mobile application support
Author

Abraham Villegas

Capstone Project: FantasyZone

A fantasy football platform built using React, Spring Boot, MySQL, ESPN NFL data, and AI-assisted fantasy football features.
