package com.fantasyzone.service;

import com.fantasyzone.TestData;
import com.fantasyzone.entity.*;
import com.fantasyzone.exception.NotFoundException;
import com.fantasyzone.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StandingsServiceTest {
    @Mock FantasyTeamRepository teams;
    @Mock LeagueRepository leagues;

    @Test
    void standingsAreRankedInRepositoryOrder() {
        League league = TestData.league(1L, 2025);
        User a = TestData.user(1L, "a@test.com", "Alice", false);
        User b = TestData.user(2L, "b@test.com", "Bob", false);
        FantasyTeam first = TestData.team(1L, "Winners", league, a);
        first.setWins(2); first.setPointsFor(150.5);
        FantasyTeam second = TestData.team(2L, "Losers", league, b);
        second.setLosses(2);
        when(leagues.existsById(1L)).thenReturn(true);
        when(teams.findStandings(1L)).thenReturn(List.of(first, second));

        List<StandingRow> rows = new StandingsService(teams, leagues).standings(1L);

        assertEquals(1, rows.get(0).rank());
        assertEquals("Winners", rows.get(0).teamName());
        assertEquals("Alice", rows.get(0).owner());
        assertEquals(150.5, rows.get(0).pointsFor(), 0.001);
        assertEquals(2, rows.get(1).rank());
    }

    @Test
    void unknownLeagueIsNotFound() {
        when(leagues.existsById(9L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> new StandingsService(teams, leagues).standings(9L));
    }
}
