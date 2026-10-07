package com.fantasyzone.service;

import com.fantasyzone.TestData;
import com.fantasyzone.entity.*;
import com.fantasyzone.exception.*;
import com.fantasyzone.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeagueAccessTest {
    @Mock UserRepository users;
    @Mock LeagueMemberRepository members;
    LeagueAccess access;

    @BeforeEach
    void setUp() {
        access = new LeagueAccess(users, members);
        when(users.findByEmail("user@test.com")).thenReturn(Optional.of(TestData.user(1L, "user@test.com", "User", false)));
        when(users.findByEmail("admin@test.com")).thenReturn(Optional.of(TestData.user(2L, "admin@test.com", "Admin", true)));
    }

    @Test
    void unknownUserIsNotFound() {
        assertThrows(NotFoundException.class, () -> access.user("ghost@test.com"));
    }

    @Test
    void adminBypassesMembershipAndCommissionerChecks() {
        assertTrue(access.isAdmin("admin@test.com"));
        assertDoesNotThrow(() -> access.requireMember(9L, "admin@test.com"));
        assertDoesNotThrow(() -> access.requireCommissioner(9L, "admin@test.com"));
    }

    @Test
    void memberPassesAndNonMemberIsForbidden() {
        when(members.existsByUserIdAndLeagueId(1L, 9L)).thenReturn(true);
        assertFalse(access.isAdmin("user@test.com"));
        assertDoesNotThrow(() -> access.requireMember(9L, "user@test.com"));
        assertThrows(ForbiddenException.class, () -> access.requireMember(8L, "user@test.com"));
    }

    @Test
    void onlyCommissionerPassesCommissionerCheck() {
        LeagueMember commissioner = new LeagueMember();
        commissioner.setCommissioner(true);
        LeagueMember regular = new LeagueMember();
        when(members.findByUserIdAndLeagueId(1L, 9L)).thenReturn(Optional.of(commissioner));
        when(members.findByUserIdAndLeagueId(1L, 8L)).thenReturn(Optional.of(regular));

        assertDoesNotThrow(() -> access.requireCommissioner(9L, "user@test.com"));
        assertThrows(ForbiddenException.class, () -> access.requireCommissioner(8L, "user@test.com"));
        assertThrows(ForbiddenException.class, () -> access.requireCommissioner(7L, "user@test.com"));
    }
}
