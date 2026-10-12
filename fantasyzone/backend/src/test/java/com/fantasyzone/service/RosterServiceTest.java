package com.fantasyzone.service;

import com.fantasyzone.dto.DTO;
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
class RosterServiceTest {
    @Mock FantasyTeamRepository teams;
    @Mock NflPlayerRepository players;
    @Mock RosterPlayerRepository roster;
    @Mock RosterTransactionRepository transactions;
    @Mock LeagueAccess access;
    RosterService service;

    FantasyTeam team;
    NflPlayer player;

    @BeforeEach
    void setUp() {
        service = new RosterService(teams, players, roster, transactions, access);
        League league = new League();
        league.setId(1L);
        User owner = new User();
        owner.setEmail("owner@test.com");
        team = new FantasyTeam();
        team.setId(10L);
        team.setLeague(league);
        team.setOwner(owner);
        player = new NflPlayer();
        player.setId(5L);
        player.setName("Josh Allen");
        player.setPosition("QB");
        player.setNflTeam("BUF");
        when(teams.findById(10L)).thenReturn(Optional.of(team));
        when(players.findById(5L)).thenReturn(Optional.of(player));
        when(roster.findByFantasyTeamId(10L)).thenReturn(new ArrayList<>());
        when(roster.save(any(RosterPlayer.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void ownerCanAddAvailablePlayer() {
        DTO.RosterEntryResponse result = service.addPlayer(10L, 5L,  "owner@test.com");
        assertEquals("QB", result.slot());
        assertEquals("Josh Allen", result.name());
    }

    @Test
    void cannotAddPlayerAlreadyTakenInLeague() {
        when(roster.isPlayerTakenInLeague(1L, 5L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.addPlayer(10L, 5L,  "owner@test.com"));
    }

    @Test
    void otherUsersCannotEditRoster() {
        assertThrows(ForbiddenException.class, () -> service.addPlayer(10L, 5L,  "intruder@test.com"));
    }

    @Test
    void lockedLeagueRejectsRosterMoves() {
        team.getLeague().setRostersLocked(true);
        assertThrows(ConflictException.class, () -> service.addPlayer(10L, 5L,  "owner@test.com"));
    }

    @Test
    void droppingPlayerNotOnRosterIsNotFound() {
        when(roster.findByFantasyTeamIdAndNflPlayerId(10L, 5L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.dropPlayer(10L, 5L, "owner@test.com"));
    }

    @Test
    void cannotAddToFullRoster() {
        when(roster.findByFantasyTeamId(10L)).thenReturn(Collections.nCopies(15, new RosterPlayer()));
        assertThrows(ConflictException.class, () -> service.addPlayer(10L, 5L,  "owner@test.com"));
    }

    @Test
    void ownerCanDropPlayer() {
        RosterPlayer spot = new RosterPlayer();
        when(roster.findByFantasyTeamIdAndNflPlayerId(10L, 5L)).thenReturn(Optional.of(spot));
        service.dropPlayer(10L, 5L, "owner@test.com");
        verify(roster).delete(spot);
    }

    @Test
    void adminCanEditAnyRoster() {
        when(access.isAdmin("admin@test.com")).thenReturn(true);
        DTO.RosterEntryResponse result = service.addPlayer(10L, 5L,  "admin@test.com");
        assertEquals("QB", result.slot());
    }

    @Test
    void addingAPlayerIsLogged() {
        service.addPlayer(10L, 5L,  "owner@test.com");
        ArgumentCaptor<RosterTransaction> saved = ArgumentCaptor.forClass(RosterTransaction.class);
        verify(transactions).save(saved.capture());
        assertEquals(TransactionType.ADD, saved.getValue().getType());
        assertEquals("QB", saved.getValue().getPositionSlot());
        assertSame(player, saved.getValue().getNflPlayer());
    }

    @Test
    void droppingAPlayerIsLogged() {
        RosterPlayer spot = new RosterPlayer();
        spot.setFantasyTeam(team);
        spot.setNflPlayer(player);
        spot.setPositionSlot("WR");
        when(roster.findByFantasyTeamIdAndNflPlayerId(10L, 5L)).thenReturn(Optional.of(spot));
        service.dropPlayer(10L, 5L, "owner@test.com");
        ArgumentCaptor<RosterTransaction> saved = ArgumentCaptor.forClass(RosterTransaction.class);
        verify(transactions).save(saved.capture());
        assertEquals(TransactionType.DROP, saved.getValue().getType());
        assertEquals("WR", saved.getValue().getPositionSlot());
    }

    @Test
    void failedMovesAreNotLogged() {
        when(roster.isPlayerTakenInLeague(1L, 5L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.addPlayer(10L, 5L,  "owner@test.com"));
        verify(transactions, never()).save(any());
    }
}
