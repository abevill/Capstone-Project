package com.fantasyzone.service;

import com.fantasyzone.TestData;
import com.fantasyzone.dto.*;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamServiceTest {
    @Mock FantasyTeamRepository teams;
    @Mock LeagueRepository leagues;
    @Mock RosterPlayerRepository roster;
    @Mock LeagueAccess access;
    TeamService service;

    User owner;
    League league;
    FantasyTeam team;

    @BeforeEach
    void setUp() {
        service = new TeamService(teams, leagues, roster, access);
        owner = TestData.user(5L, "owner@test.com", "Owner", false);
        league = TestData.league(1L, 2025);
        team = TestData.team(10L, "Old Name", league, owner);
        when(access.user("owner@test.com")).thenReturn(owner);
        when(leagues.findById(1L)).thenReturn(Optional.of(league));
        when(teams.findById(10L)).thenReturn(Optional.of(team));
        when(teams.save(any(FantasyTeam.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void memberCanCreateTeam() {
        TeamResponse res = service.create(1L, new TeamRequest("  Gridiron Gang "), "owner@test.com");
        assertEquals("Gridiron Gang", res.name());
        assertEquals("Owner", res.owner());
        assertEquals(1L, res.leagueId());
    }

    @Test
    void onlyOneTeamPerUserPerLeague() {
        when(teams.existsByLeagueIdAndOwnerId(1L, 5L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(1L, new TeamRequest("X"), "owner@test.com"));
    }

    @Test
    void teamNamesAreUniqueWithinLeague() {
        when(teams.existsByLeagueIdAndName(1L, "Taken")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(1L, new TeamRequest("Taken"), "owner@test.com"));
    }

    @Test
    void createInUnknownLeagueIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.create(99L, new TeamRequest("X"), "owner@test.com"));
    }

    @Test
    void ownerCanRename() {
        TeamResponse res = service.rename(10L, new TeamRequest("New Name"), "owner@test.com");
        assertEquals("New Name", res.name());
    }

    @Test
    void otherUsersCannotRename() {
        assertThrows(ForbiddenException.class, () -> service.rename(10L, new TeamRequest("Hack"), "other@test.com"));
    }

    @Test
    void renameRejectsTakenName() {
        when(teams.existsByLeagueIdAndName(1L, "Taken")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.rename(10L, new TeamRequest("Taken"), "owner@test.com"));
    }

    @Test
    void getUnknownTeamIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.get(99L, "owner@test.com"));
    }

    @Test
    void rosterIsMappedToEntries() {
        NflPlayer p = TestData.player(7L, "Josh Allen", "QB", "BUF");
        when(roster.findByFantasyTeamId(10L)).thenReturn(List.of(TestData.spot(team, p, "QB")));
        List<RosterEntryResponse> res = service.roster(10L, "owner@test.com");
        assertEquals("Josh Allen", res.get(0).name());
        assertEquals("QB", res.get(0).slot());
        verify(access).requireMember(1L, "owner@test.com");
    }

    @Test
    void listForLeagueAndMineMapTeams() {
        when(teams.findByLeagueId(1L)).thenReturn(List.of(team));
        when(teams.findByOwnerId(5L)).thenReturn(List.of(team));
        assertEquals(1, service.listForLeague(1L, "owner@test.com").size());
        assertEquals("Old Name", service.mine("owner@test.com").get(0).name());
    }
}
