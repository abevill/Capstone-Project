package com.fantasyzone.service;

import com.fantasyzone.dto.DTO;
import com.fantasyzone.dto.DTO.*;
import com.fantasyzone.entity.NflPlayer;
import com.fantasyzone.entity.PlayerWeeklyStats;
import com.fantasyzone.exception.NotFoundException;
import com.fantasyzone.repository.NflPlayerRepository;
import com.fantasyzone.repository.PlayerWeeklyStatsRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gathers the FantasyZone data the assistant is allowed to see and turns it into plain text.
 * It reuses the existing services, so the usual access rules apply: asking about a team requires being in that
 * team's league. Email addresses and password data are never included.
 * This is the ONLY new class that depends on existing FantasyZone code.
 */
@Service
public class AssistantContextBuilder {
    static final int RECENT_GAMES = 4;
    static final String SCORING_RULES =
        "League scoring (fixed): passing yard 0.04, passing TD 4, interception -2, rushing yard 0.1, rushing TD 6, "
        + "reception 1, receiving yard 0.1, receiving TD 6, fumble lost -2. Bench players do not score.";

    public record Context(String text, List<String> dataUsed) {}

    private final TeamService teams;
    private final StandingsService standings;
    private final MatchupService matchups;
    private final NflPlayerRepository players;
    private final PlayerWeeklyStatsRepository stats;
    private final FantasyScoringService scoring;

    public AssistantContextBuilder(TeamService teams, StandingsService standings, MatchupService matchups,
                                   NflPlayerRepository players, PlayerWeeklyStatsRepository stats,
                                   FantasyScoringService scoring) {
        this.teams = teams; this.standings = standings; this.matchups = matchups;
        this.players = players; this.stats = stats; this.scoring = scoring;
    }

    public Context build(AssistantRequest req, String email) {
        StringBuilder text = new StringBuilder(SCORING_RULES).append('\n');
        List<String> used = new ArrayList<>();
        used.add("League scoring rules");

        boolean hasTeam = req.teamId() != null;
        boolean hasPlayers = req.playerIds() != null && !req.playerIds().isEmpty();
        if (hasTeam) appendTeam(text, used, req, email);
        if (hasPlayers) appendPlayers(text, used, req.playerIds());
        if (!hasTeam && !hasPlayers) {
            text.append("\nNo team or players were selected, so no team or player data is available.\n");
        }
        return new Context(text.toString(), used);
    }

    // ---------- team ----------
    private void appendTeam(StringBuilder text, List<String> used, AssistantRequest req, String email) {
        TeamResponse team = teams.get(req.teamId(), email);            // 404 if missing, 403 if not in the league
        List<RosterEntryResponse> roster = teams.roster(team.id(), email);

        text.append("\nTEAM: ").append(clean(team.name())).append(" (manager ").append(clean(team.owner())).append(")\n");
        text.append("Record: ").append(record(team.wins(), team.losses(), team.ties()))
            .append(", points scored ").append(fmt(team.pointsFor())).append('\n');
        text.append("Roster (slot: player, position, NFL team, recent fantasy points):\n");
        if (roster.isEmpty()) text.append("- (empty roster)\n");
        for (RosterEntryResponse r : roster) {
            text.append("- ").append(clean(r.slot())).append(": ").append(clean(r.name()))
                .append(" (").append(clean(r.position())).append(", ").append(clean(r.nflTeam())).append(") ")
                .append(recent(r.playerId())).append('\n');
        }
        used.add("Team \"" + clean(team.name()) + "\" with " + roster.size() + " roster players");

        List<StandingRow> table = standings.standings(team.leagueId());
        text.append("League standings (top 5):\n");
        table.stream().limit(5).forEach(row -> text.append("#").append(row.rank()).append(' ').append(clean(row.teamName()))
            .append(' ').append(record(row.wins(), row.losses(), row.ties()))
            .append(" (").append(fmt(row.pointsFor())).append(" pts)")
            .append(Objects.equals(row.teamId(), team.id()) ? " <- this team" : "").append('\n'));
        used.add("League standings");

        List<MatchupResponse> mine = matchups.list(team.leagueId(), req.week(), email).stream()
            .filter(m -> Objects.equals(m.homeTeamId(), team.id()) || Objects.equals(m.awayTeamId(), team.id()))
            .toList();
        Optional<MatchupResponse> chosen = req.week() != null
            ? mine.stream().findFirst()
            : mine.stream().filter(m -> !m.finalized()).findFirst();
        chosen.ifPresent(m -> {
            text.append("Matchup, week ").append(m.week()).append(": ").append(clean(m.homeTeam())).append(" vs ")
                .append(clean(m.awayTeam())).append(m.finalized()
                    ? " (final " + fmt(m.homeScore()) + " to " + fmt(m.awayScore()) + ")\n"
                    : " (not scored yet)\n");
            used.add("Matchup for week " + m.week());
        });
    }

    // ---------- players ----------
    private void appendPlayers(StringBuilder text, List<String> used, List<Long> ids) {
        text.append("\nPLAYERS TO CONSIDER:\n");
        for (Long id : new LinkedHashSet<>(ids)) {
            NflPlayer p = players.findById(id).orElseThrow(() -> new NotFoundException("Player " + id + " not found"));
            text.append("- ").append(clean(p.getName())).append(" (").append(clean(p.getPosition())).append(", ")
                .append(clean(p.getNflTeam())).append(") ").append(recent(id)).append('\n');
            used.add("Recent stats for " + clean(p.getName()));
        }
    }

    // ---------- helpers ----------
    private String recent(Long playerId) {
        List<PlayerWeeklyStats> lines = stats.findByNflPlayerId(playerId).stream()
            .sorted(Comparator.comparingInt(PlayerWeeklyStats::getSeason).thenComparingInt(PlayerWeeklyStats::getWeek).reversed())
            .limit(RECENT_GAMES)
            .toList();
        if (lines.isEmpty()) return "no stats on record";
        double average = lines.stream().mapToDouble(scoring::calculateFantasyPoints).average().orElse(0);
        String detail = lines.stream()
            .map(s -> "wk" + s.getWeek() + " (" + s.getSeason() + "): " + fmt(scoring.calculateFantasyPoints(s)) + " pts [" + summarize(s) + "]")
            .collect(Collectors.joining("; "));
        return "last " + lines.size() + " games, average " + fmt(average) + " pts: " + detail;
    }

    private static String summarize(PlayerWeeklyStats s) {
        List<String> parts = new ArrayList<>();
        add(parts, s.getPassingYards(), "pass yds");
        add(parts, s.getPassingTouchdowns(), "pass TD");
        add(parts, s.getInterceptions(), "INT");
        add(parts, s.getRushingYards(), "rush yds");
        add(parts, s.getRushingTouchdowns(), "rush TD");
        add(parts, s.getReceptions(), "rec");
        add(parts, s.getReceivingYards(), "rec yds");
        add(parts, s.getReceivingTouchdowns(), "rec TD");
        add(parts, s.getFumblesLost(), "fumbles lost");
        return parts.isEmpty() ? "no offensive stats" : String.join(", ", parts);
    }

    private static void add(List<String> parts, int value, String label) {
        if (value != 0) parts.add(value + " " + label);
    }

    private static String record(int wins, int losses, int ties) {
        return ties > 0 ? wins + "-" + losses + "-" + ties : wins + "-" + losses;
    }

    private static String fmt(Double value) {
        return String.format(Locale.US, "%.1f", value == null ? 0.0 : value);
    }

    /** Names can come from users, so strip line breaks and angle brackets (they could imitate our prompt markers). */
    private static String clean(String value) {
        if (value == null) return "n/a";
        String cleaned = value.replaceAll("[\\r\\n<>]+", " ").trim();
        return cleaned.length() > 80 ? cleaned.substring(0, 80) : cleaned;
    }
}
