package com.fantasyzone.service;

import com.fantasyzone.dto.DTO;
import com.fantasyzone.entity.*;
import com.fantasyzone.exception.*;
import com.fantasyzone.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class MatchupService {
    private final MatchupRepository matchups;
    private final FantasyTeamRepository teams;
    private final LeagueRepository leagues;
    private final RosterPlayerRepository roster;
    private final PlayerWeeklyStatsRepository stats;
    private final FantasyScoringService scoring;
    private final LeagueAccess access;

    public MatchupService(MatchupRepository matchups, FantasyTeamRepository teams, LeagueRepository leagues,
                          RosterPlayerRepository roster, PlayerWeeklyStatsRepository stats,
                          FantasyScoringService scoring, LeagueAccess access) {
        this.matchups = matchups; this.teams = teams; this.leagues = leagues;
        this.roster = roster; this.stats = stats; this.scoring = scoring; this.access = access;
    }

    @Transactional(readOnly = true)
    public List<DTO.MatchupResponse> list(Long leagueId, Integer week, String email) {
        access.requireMember(leagueId, email);
        List<Matchup> found = week == null
            ? matchups.findByLeagueIdOrderByWeek(leagueId)
            : matchups.findByLeagueIdAndWeek(leagueId, week);
        return found.stream().map(this::toResponse).toList();
    }

    /** Single round-robin schedule using the circle method: n teams produce n-1 weeks. */
    @Transactional
    public List<DTO.MatchupResponse> generate(Long leagueId, String email) {
        League league = leagues.findById(leagueId).orElseThrow(() -> new NotFoundException("League not found"));
        access.requireCommissioner(leagueId, email);
        if (matchups.existsByLeagueId(leagueId)) throw new ConflictException("Matchups were already generated");
        List<FantasyTeam> list = new ArrayList<>(teams.findByLeagueId(leagueId));
        int n = list.size();
        if (n < 2 || n % 2 != 0)
            throw new ConflictException("A league needs an even number of teams (at least 2) to generate matchups");

        List<Matchup> out = new ArrayList<>();
        for (int week = 1; week < n; week++) {
            for (int i = 0; i < n / 2; i++) {
                Matchup m = new Matchup();
                m.setLeague(league);
                m.setWeek(week);
                m.setHomeTeam(list.get(i));
                m.setAwayTeam(list.get(n - 1 - i));
                out.add(m);
            }
            list.add(1, list.remove(n - 1)); // rotate everyone except the first team
        }
        return matchups.saveAll(out).stream().map(this::toResponse).toList();
    }

    /** Calculates both scores from stored stats, then updates wins/losses/ties. Can only run once per matchup. */
    @Transactional
    public DTO.MatchupResponse calculate(Long matchupId, String email) {
        Matchup m = matchups.findById(matchupId).orElseThrow(() -> new NotFoundException("Matchup not found"));
        access.requireCommissioner(m.getLeague().getId(), email);
        if (m.isFinalized()) throw new ConflictException("This matchup is already finalized");

        int season = m.getLeague().getSeason();
        double home = teamScore(m.getHomeTeam(), season, m.getWeek());
        double away = teamScore(m.getAwayTeam(), season, m.getWeek());
        m.setHomeScore(home);
        m.setAwayScore(away);
        m.setFinalized(true);

        FantasyTeam h = m.getHomeTeam(), a = m.getAwayTeam();
        h.setPointsFor(round(h.getPointsFor() + home));
        a.setPointsFor(round(a.getPointsFor() + away));
        if (home > away) { h.setWins(h.getWins() + 1); a.setLosses(a.getLosses() + 1); }
        else if (away > home) { a.setWins(a.getWins() + 1); h.setLosses(h.getLosses() + 1); }
        else { h.setTies(h.getTies() + 1); a.setTies(a.getTies() + 1); }
        return toResponse(m);
    }

    private double teamScore(FantasyTeam team, int season, int week) {
        double total = 0;
        for (RosterPlayer rp : roster.findByFantasyTeamId(team.getId())) {
            if ("BENCH".equalsIgnoreCase(rp.getPositionSlot())) continue;
            total += stats.findByNflPlayerIdAndSeasonAndWeek(rp.getNflPlayer().getId(), season, week)
                .map(scoring::calculateFantasyPoints).orElse(0.0);
        }
        return round(total);
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }

    private DTO.MatchupResponse toResponse(Matchup m) {
        return new DTO.MatchupResponse(m.getId(), m.getWeek(),
            m.getHomeTeam().getId(), m.getHomeTeam().getName(),
            m.getAwayTeam().getId(), m.getAwayTeam().getName(),
            m.getHomeScore(), m.getAwayScore(), m.isFinalized());
    }
}
