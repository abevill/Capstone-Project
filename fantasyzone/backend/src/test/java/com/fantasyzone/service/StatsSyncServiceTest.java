package com.fantasyzone.service;

import com.fantasyzone.TestData;
import com.fantasyzone.dto.DTO;
import com.fantasyzone.dto.DTO.*;
import com.fantasyzone.entity.*;
import com.fantasyzone.exception.*;
import com.fantasyzone.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatsSyncServiceTest {
    @Mock SportsDataService sports;
    @Mock NflPlayerRepository players;
    @Mock PlayerWeeklyStatsRepository stats;
    @Captor ArgumentCaptor<List<NflPlayer>> saved;
    StatsSyncService service;

    NflPlayer known;

    @BeforeEach
    void setUp() {
        service = new StatsSyncService(sports, players, stats, new TransactionTemplate(mock(PlatformTransactionManager.class)));
        known = TestData.player(1L, "Old Name", "QB", "AAA");
        known.setExternalId("100");
        when(players.findAll()).thenReturn(List.of(known));
    }

    private static PlayerWeeklyStatsDto dto(String id, int passYds) {
        return new PlayerWeeklyStatsDto(id, 2026, 4, passYds, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    // ---------- players ----------
    @Test
    void newPlayersAreCreatedChangedOnesUpdatedAndSameOnesLeftAlone() {
        NflPlayer same = TestData.player(2L, "Same Guy", "RB", "BBB");
        same.setExternalId("200");
        when(players.findAll()).thenReturn(List.of(known, same));
        when(sports.getPlayers()).thenReturn(List.of(
            new NflPlayerDto("100", "New Name", "QB", "CCC"),   // renamed and moved teams
            new NflPlayerDto("200", "Same Guy", "RB", "BBB"),   // unchanged
            new NflPlayerDto("300", "Rookie", "WR", "DDD")));   // new

        SyncResult result = service.syncPlayers();

        assertEquals(1, result.playersCreated());
        assertEquals(1, result.playersUpdated());
        assertEquals("New Name", known.getName());
        assertEquals("CCC", known.getNflTeam());
        verify(players).saveAll(saved.capture());
        assertEquals(1, saved.getValue().size());
        assertEquals("300", saved.getValue().get(0).getExternalId());
    }

    @Test
    void providerFailureLeavesTheDatabaseUntouched() {
        when(sports.getPlayers()).thenThrow(new SportsDataException("down", null));
        assertThrows(SportsDataException.class, () -> service.syncPlayers());
        verify(players, never()).saveAll(anyList());
    }

    // ---------- stats ----------
    @Test
    void statLinesAreCreatedUpdatedAndUnknownPlayersSkipped() {
        NflPlayer other = TestData.player(2L, "Other", "WR", "BBB");
        other.setExternalId("200");
        when(players.findAll()).thenReturn(List.of(known, other));
        PlayerWeeklyStats existing = new PlayerWeeklyStats();
        existing.setPassingYards(100);
        when(stats.findByNflPlayerIdAndSeasonAndWeek(1L, 2026, 4)).thenReturn(Optional.of(existing));
        when(sports.getWeeklyStats(2026, 4)).thenReturn(List.of(dto("100", 299), dto("200", 50), dto("999", 10)));

        SyncResult result = service.syncStats(2026, 4);

        assertEquals(1, result.statLinesCreated());
        assertEquals(1, result.statLinesUpdated());
        assertEquals(1, result.skippedUnknownPlayers());
        assertEquals(299, existing.getPassingYards());
        verify(stats, times(2)).save(any(PlayerWeeklyStats.class));
    }

    @Test
    void runningTheSameStatsSyncTwiceChangesNothing() {
        PlayerWeeklyStats existing = new PlayerWeeklyStats();
        existing.setPassingYards(299);
        when(stats.findByNflPlayerIdAndSeasonAndWeek(1L, 2026, 4)).thenReturn(Optional.of(existing));
        when(sports.getWeeklyStats(2026, 4)).thenReturn(List.of(dto("100", 299)));

        SyncResult result = service.syncStats(2026, 4);

        assertEquals(0, result.statLinesCreated());
        assertEquals(0, result.statLinesUpdated());
        verify(stats, never()).save(any());
    }

    @Test
    void missingSeasonOrWeekUsesTheProvidersCurrentWeek() {
        when(sports.currentWeek()).thenReturn(new SeasonWeek(2026, 5));
        when(sports.getWeeklyStats(2026, 5)).thenReturn(List.of());
        service.syncStats(null, null);
        service.syncStats(2026, null);
        verify(sports, times(2)).getWeeklyStats(2026, 5);
    }

    @Test
    void invalidSeasonOrWeekIsRejectedBeforeAnyNetworkCall() {
        assertThrows(BadRequestException.class, () -> service.syncStats(2026, 0));
        assertThrows(BadRequestException.class, () -> service.syncStats(2026, 19));
        assertThrows(BadRequestException.class, () -> service.syncStats(1999, 4));
        verify(sports, never()).getWeeklyStats(anyInt(), anyInt());
    }

    @Test
    void statsProviderFailureSavesNothing() {
        when(sports.getWeeklyStats(2026, 4)).thenThrow(new SportsDataException("down", null));
        assertThrows(SportsDataException.class, () -> service.syncStats(2026, 4));
        verify(stats, never()).save(any());
    }
}
