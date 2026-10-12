package com.fantasyzone.service;

import com.fantasyzone.TestData;
import com.fantasyzone.entity.User;
import com.fantasyzone.exception.*;
import com.fantasyzone.repository.UserRepository;
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
class AdminServiceTest {
    @Mock UserRepository users;
    AdminService service;
    User bob;

    @BeforeEach
    void setUp() {
        service = new AdminService(users);
        bob = TestData.user(2L, "bob@test.com", "Bob", false);
        when(users.findById(2L)).thenReturn(Optional.of(bob));
    }

    @Test
    void listUsersMapsSummaries() {
        when(users.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(bob)));
        Page<UserSummary> page = service.listUsers(PageRequest.of(0, 20));
        assertEquals("bob@test.com", page.getContent().get(0).email());
        assertEquals(List.of("ROLE_USER"), page.getContent().get(0).roles());
    }

    @Test
    void adminCanDisableAndEnableAnotherUser() {
        assertFalse(service.setEnabled(2L, false, "admin@test.com").enabled());
        assertFalse(bob.isEnabled());
        assertTrue(service.setEnabled(2L, true, "admin@test.com").enabled());
    }

    @Test
    void adminCannotDisableOwnAccount() {
        assertThrows(ConflictException.class, () -> service.setEnabled(2L, false, "BOB@test.com"));
        assertTrue(bob.isEnabled());
    }

    @Test
    void unknownUserIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.setEnabled(99L, false, "admin@test.com"));
    }
}
