package com.fantasyzone.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

/**
 * Every request and response record in one place. JSON field names, validation rules, and behavior are
 * identical to the separate files this replaces. Use them as Dtos.TeamResponse, or add
 * "import com.fantasyzone.dto.Dtos.*;" to a file to use the short names.
 */
public final class DTO {
    private DTO() {}

    // ---------- auth and users ----------
    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 3, max = 50) String displayName,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record AuthResponse(Long userId, String token, String email, String displayName, List<String> roles) {}

    public record UserSummary(Long id, String email, String displayName, boolean enabled, List<String> roles) {}

    // ---------- leagues ----------
    public record CreateLeagueRequest(@NotBlank @Size(max = 80) String name, @Min(2000) @Max(2100) int season) {}

    public record JoinLeagueRequest(@NotBlank String joinCode) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LeagueResponse(Long id, String name, int season, boolean rostersLocked, long memberCount, String joinCode) {}

    public record MemberResponse(Long userId, String displayName, boolean commissioner) {}

    // ---------- teams and rosters ----------
    public record TeamRequest(@NotBlank @Size(max = 60) String name) {}

    public record TeamResponse(Long id, String name, Long leagueId, String owner, int wins, int losses, int ties, double pointsFor) {}



    public record RosterEntryResponse(Long playerId, String name, String position, String nflTeam, String slot) {}

    public record TransactionResponse(Long id, String type, Long playerId, String playerName, String position,
                                      String slot, String madeBy, Instant at) {}

    // ---------- matchups and standings ----------
    public record MatchupResponse(Long id, int week, Long homeTeamId, String homeTeam, Long awayTeamId, String awayTeam,
                                  Double homeScore, Double awayScore, boolean finalized) {}

    public record StandingRow(int rank, Long teamId, String teamName, String owner, int wins, int losses, int ties, double pointsFor) {}

    // ---------- players and stats ----------
    public record NflPlayerDto(String externalId, String name, String position, String nflTeam) {}

    public record PlayerWeeklyStatsDto(String externalPlayerId, int season, int week,
                                       int passingYards, int passingTouchdowns, int interceptions,
                                       int rushingYards, int rushingTouchdowns,
                                       int receptions, int receivingYards, int receivingTouchdowns, int fumblesLost) {}

    public record StatLineResponse(int season, int week, double fantasyPoints, PlayerWeeklyStatsDto stats) {}

    public record WeeklyStatsResponse(int season, int week,
                                      int passingYards, int passingTouchdowns, int interceptions,
                                      int rushingYards, int rushingTouchdowns,
                                      int receptions, int receivingYards, int receivingTouchdowns,
                                      int fumblesLost, double fantasyPoints) {}

    public record PlayersStatsResponse(Long playerId, String name, String position, String nflTeam,
                                       double totalFantasyPoints, List<WeeklyStatsResponse> weeklyStats) {}

    public record SeasonWeek(int season, int week) {}
    // ---------- data sync ----------
    public record SyncResult(int playersCreated, int playersUpdated, int statLinesCreated, int statLinesUpdated,
                             int skippedUnknownPlayers) {
        public static SyncResult players(int created, int updated) { return new SyncResult(created, updated, 0, 0, 0); }
        public static SyncResult stats(int created, int updated, int skipped) { return new SyncResult(0, 0, created, updated, skipped); }
    }

    // ---------- AI assistant ----------
    /** teamId: include that team's data (caller must be in its league). playerIds: up to 6 players to compare. week: matchup week. */
    public record AssistantRequest(
            @NotBlank @Size(max = 500) String question,
            Long teamId,
            @Size(max = 6) List<Long> playerIds,
            @Min(1) @Max(18) Integer week) {}

    public record AssistantResponse(String answer, String model, List<String> dataUsed) {}
}