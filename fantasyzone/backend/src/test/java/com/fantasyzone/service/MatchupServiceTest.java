package com.fantasyzone.service;

import com.fantasyzone.TestData;
import com.fantasyzone.dto.MatchupResponse;
import com.fantasyzone.entity.*;
import com.fantasyzone.exception.*;
import com.fantasyzone.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchupServiceTest {
    @Mock MatchupRepository matchups;
    @Mock FantasyTeamRepository teams;
    @Mock LeagueRepository leagues;
    @Mock RosterPlayerRepository roster;
    @Mock PlayerWeeklyStatsRepository stats;
    @Mock LeagueAccess access;
    MatchupService service;

    League league;
    User owner;

    @BeforeEach
    void setUp() {
        service = new MatchupService(matchups, teams, leagues, roster, stats, new FantasyScoringService(), access);
        league = TestData.league(1L, 2025);
        owner = TestData.user(1L, "o@test.com", "Owner", false);
        when(leagues.findById(1L)).thenReturn(Optional.of(league));
        when(matchups.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private List<FantasyTeam> teamsOf(int n) {
        List<FantasyTeam> list = new ArrayList<>();
        for (long i = 1; i <= n; i++) list.add(TestData.team(i, "Team " + i, league, owner));
        return list;
    }

    private Matchup matchup(FantasyTeam home, FantasyTeam away) {
        Matchup m = new Matchup();
        m.setId(100L);
        m.setLeague(league);
        m.setWeek(1);
        m.setHomeTeam(home);
        m.setAwayTeam(away);
        when(matchups.findById(100L)).thenReturn(Optional.of(m));
        return m;
    }

    // ---- schedule generation ----
    @Test
    void fourTeamsPlayEachOtherExactlyOnceAndOncePerWeek() {
        when(teams.findByLeagueId(1L)).thenReturn(teamsOf(4));
        List<MatchupResponse> out = service.generate(1L, "o@test.com");

        assertEquals(6, out.size());
        Set<String> pairs = new HashSet<>();
        Map<Integer, Set<Long>> perWeek = new HashMap<>();
        for (MatchupResponse m : out) {
            pairs.add(Math.min(m.homeTeamId(), m.awayTeamId()) + "-" + Math.max(m.homeTeamId(), m.awayTeamId()));
            Set<Long> seen = perWeek.computeIfAbsent(m.week(), w -> new HashSet<>());
            assertTrue(seen.add(m.homeTeamId()), "team plays twice in one week");
            assertTrue(seen.add(m.awayTeamId()), "team plays twice in one week");
        }
        assertEquals(6, pairs.size());
        assertEquals(Set.of(1, 2, 3), perWeek.keySet());
        verify(access).requireCommissioner(1L, "o@test.com");
    }

    @Test
    void oddNumberOfTeamsCannotBeScheduled() {
        when(teams.findByLeagueId(1L)).thenReturn(teamsOf(3));
        assertThrows(ConflictException.class, () -> service.generate(1L, "o@test.com"));
    }

    @Test
    void singleTeamCannotBeScheduled() {
        when(teams.findByLeagueId(1L)).thenReturn(teamsOf(1));
        assertThrows(ConflictException.class, () -> service.generate(1L, "o@test.com"));
    }

    @Test
    void matchupsCanOnlyBeGeneratedOnce() {
        when(matchups.existsByLeagueId(1L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.generate(1L, "o@test.com"));
    }

    @Test
    void generateForUnknownLeagueIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.generate(99L, "o@test.com"));
    }

    // ---- scoring ----
    @Test
    void calculateScoresStartersIgnoresBenchAndUpdatesRecords() {
        FantasyTeam home = teamsOf(2).get(0);
        FantasyTeam away = teamsOf(2).get(1);
        matchup(home, away);

        NflPlayer qb = TestData.player(10L, "QB", "QB", "AAA");
        NflPlayer benchStar = TestData.player(11L, "Bench", "WR", "BBB");
        NflPlayer wr = TestData.player(12L, "WR", "WR", "CCC");
        when(roster.findByFantasyTeamId(1L)).thenReturn(List.of(TestData.spot(home, qb, "QB")));
        when(roster.findByFantasyTeamId(2L)).thenReturn(List.of(
            TestData.spot(away, benchStar, "BENCH"), TestData.spot(away, wr, "WR")));

        PlayerWeeklyStats qbStats = new PlayerWeeklyStats();
        qbStats.setPassingYards(250);       // 10.0
        qbStats.setPassingTouchdowns(2);    // 8.0
        PlayerWeeklyStats benchStats = new PlayerWeeklyStats();
        benchStats.setReceivingTouchdowns(5); // 30 points that must NOT count
        PlayerWeeklyStats wrStats = new PlayerWeeklyStats();
        wrStats.setReceptions(5);           // 5.0
        wrStats.setReceivingYards(50);      // 5.0
        when(stats.findByNflPlayerIdAndSeasonAndWeek(10L, 2025, 1)).thenReturn(Optional.of(qbStats));
        when(stats.findByNflPlayerIdAndSeasonAndWeek(11L, 2025, 1)).thenReturn(Optional.of(benchStats));
        when(stats.findByNflPlayerIdAndSeasonAndWeek(12L, 2025, 1)).thenReturn(Optional.of(wrStats));

        MatchupResponse res = service.calculate(100L, "o@test.com");

        assertEquals(18.0, res.homeScore(), 0.001);
        assertEquals(10.0, res.awayScore(), 0.001);
        assertTrue(res.finalized());
        assertEquals(1, home.getWins());
        assertEquals(1, away.getLosses());
        assertEquals(18.0, home.getPointsFor(), 0.001);
        assertEquals(10.0, away.getPointsFor(), 0.001);
    }

    @Test
    void awayWinAndTieUpdateRecordsCorrectly() {
        FantasyTeam home = teamsOf(2).get(0);
        FantasyTeam away = teamsOf(2).get(1);
        matchup(home, away);
        NflPlayer wr = TestData.player(12L, "WR", "WR", "CCC");
        when(roster.findByFantasyTeamId(2L)).thenReturn(List.of(TestData.spot(away, wr, "WR")));
        PlayerWeeklyStats s = new PlayerWeeklyStats();
        s.setReceptions(3);
        when(stats.findByNflPlayerIdAndSeasonAndWeek(12L, 2025, 1)).thenReturn(Optional.of(s));
        service.calculate(100L, "o@test.com");
        assertEquals(1, away.getWins());
        assertEquals(1, home.getLosses());

        FantasyTeam t1 = teamsOf(2).get(0);
        FantasyTeam t2 = teamsOf(2).get(1);
        matchup(t1, t2);
        when(roster.findByFantasyTeamId(2L)).thenReturn(List.of());
        service.calculate(100L, "o@test.com");
        assertEquals(1, t1.getTies());
        assertEquals(1, t2.getTies());
    }

    @Test
    void finalizedMatchupCannotBeCalculatedTwice() {
        Matchup m = matchup(teamsOf(2).get(0), teamsOf(2).get(1));
        m.setFinalized(true);
        assertThrows(ConflictException.class, () -> service.calculate(100L, "o@test.com"));
    }

    @Test
    void calculateUnknownMatchupIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.calculate(404L, "o@test.com"));
    }

    // ---- listing ----
    @Test
    void listCanFilterByWeek() {
        List<FantasyTeam> t = teamsOf(2);
        Matchup m = matchup(t.get(0), t.get(1));
        when(matchups.findByLeagueIdOrderByWeek(1L)).thenReturn(List.of(m));
        when(matchups.findByLeagueIdAndWeek(1L, 2)).thenReturn(List.of());

        assertEquals(1, service.list(1L, null, "o@test.com").size());
        assertEquals(0, service.list(1L, 2, "o@test.com").size());
        verify(access, times(2)).requireMember(1L, "o@test.com");
    }
}
