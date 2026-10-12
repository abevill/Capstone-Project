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
import org.springframework.data.domain.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeagueServiceTest {
    @Mock LeagueRepository leagues;
    @Mock LeagueMemberRepository members;
    @Mock FantasyTeamRepository teams;
    @Mock MatchupRepository matchups;
    @Mock LeagueAccess access;
    LeagueService service;

    User user;
    League league;

    @BeforeEach
    void setUp() {
        service = new LeagueService(leagues, members, teams, matchups, access);
        user = TestData.user(5L, "me@test.com", "Me", false);
        league = TestData.league(1L, 2025);
        when(access.user("me@test.com")).thenReturn(user);
        when(leagues.findById(1L)).thenReturn(Optional.of(league));
        when(leagues.save(any(League.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void creatorBecomesCommissionerAndGetsJoinCode() {
        LeagueResponse res = service.create(new CreateLeagueRequest(" Friends ", 2025), "me@test.com");

        assertEquals("Friends", res.name());
        assertNotNull(res.joinCode());
        assertEquals(8, res.joinCode().length());
        ArgumentCaptor<LeagueMember> saved = ArgumentCaptor.forClass(LeagueMember.class);
        verify(members).save(saved.capture());
        assertTrue(saved.getValue().isCommissioner());
        assertSame(user, saved.getValue().getUser());
    }

    @Test
    void joinWithValidCodeAddsRegularMember() {
        when(leagues.findByJoinCode("CODE1")).thenReturn(Optional.of(league));
        LeagueResponse res = service.join("code1", "me@test.com");

        assertNull(res.joinCode());
        ArgumentCaptor<LeagueMember> saved = ArgumentCaptor.forClass(LeagueMember.class);
        verify(members).save(saved.capture());
        assertFalse(saved.getValue().isCommissioner());
    }

    @Test
    void joinWithUnknownCodeIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.join("nope", "me@test.com"));
    }

    @Test
    void cannotJoinTwice() {
        when(leagues.findByJoinCode("CODE1")).thenReturn(Optional.of(league));
        when(members.existsByUserIdAndLeagueId(5L, 1L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.join("CODE1", "me@test.com"));
    }

    @Test
    void getUnknownLeagueIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.get(99L));
    }

    @Test
    void listMapsPageOfLeagues() {
        when(leagues.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(league)));
        when(members.countByLeagueId(1L)).thenReturn(3L);
        Page<LeagueResponse> page = service.list(PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
        assertEquals(3L, page.getContent().get(0).memberCount());
    }

    @Test
    void membersRequireMembershipCheckThenMapResults() {
        LeagueMember m = new LeagueMember();
        m.setUser(user);
        m.setCommissioner(true);
        when(members.findByLeagueId(1L)).thenReturn(List.of(m));
        List<MemberResponse> res = service.members(1L, "me@test.com");
        assertEquals("Me", res.get(0).displayName());
        assertTrue(res.get(0).commissioner());
        verify(access).requireMember(1L, "me@test.com");
    }

    @Test
    void commissionerCannotBeRemoved() {
        LeagueMember target = new LeagueMember();
        target.setCommissioner(true);
        when(members.findByUserIdAndLeagueId(2L, 1L)).thenReturn(Optional.of(target));
        assertThrows(ConflictException.class, () -> service.removeMember(1L, 2L, "me@test.com"));
    }

    @Test
    void cannotRemoveMemberWhoseTeamHasMatchups() {
        LeagueMember target = new LeagueMember();
        FantasyTeam team = TestData.team(9L, "T", league, user);
        when(members.findByUserIdAndLeagueId(2L, 1L)).thenReturn(Optional.of(target));
        when(teams.findByLeagueIdAndOwnerId(1L, 2L)).thenReturn(Optional.of(team));
        when(matchups.existsByHomeTeamIdOrAwayTeamId(9L, 9L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.removeMember(1L, 2L, "me@test.com"));
        verify(members, never()).delete(any());
    }

    @Test
    void removingMemberDeletesTheirTeamAndMembership() {
        LeagueMember target = new LeagueMember();
        FantasyTeam team = TestData.team(9L, "T", league, user);
        when(members.findByUserIdAndLeagueId(2L, 1L)).thenReturn(Optional.of(target));
        when(teams.findByLeagueIdAndOwnerId(1L, 2L)).thenReturn(Optional.of(team));
        service.removeMember(1L, 2L, "me@test.com");
        verify(teams).delete(team);
        verify(members).delete(target);
    }

    @Test
    void removingUnknownMemberIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.removeMember(1L, 2L, "me@test.com"));
    }

    @Test
    void commissionerCanLockRosters() {
        LeagueResponse res = service.setRosterLock(1L, true, "me@test.com");
        assertTrue(res.rostersLocked());
        assertTrue(league.isRostersLocked());
    }

    @Test
    void mineIncludesJoinCodeOnlyForCommissioner() {
        LeagueMember commissioner = new LeagueMember();
        commissioner.setLeague(league);
        commissioner.setCommissioner(true);
        League other = TestData.league(2L, 2025);
        LeagueMember regular = new LeagueMember();
        regular.setLeague(other);
        when(members.findByUserId(5L)).thenReturn(List.of(commissioner, regular));

        List<LeagueResponse> res = service.mine("me@test.com");
        assertEquals("CODE1", res.get(0).joinCode());
        assertNull(res.get(1).joinCode());
    }
}
