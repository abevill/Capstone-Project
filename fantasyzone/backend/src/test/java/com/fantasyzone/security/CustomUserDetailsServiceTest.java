package com.fantasyzone.security;

import com.fantasyzone.TestData;
import com.fantasyzone.entity.User;
import com.fantasyzone.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(users);

    @Test
    void mapsRolesToAuthorities() {
        when(users.findByEmail("admin@test.com")).thenReturn(Optional.of(TestData.user(1L, "admin@test.com", "Admin", true)));
        UserDetails d = service.loadUserByUsername("admin@test.com");
        assertEquals("hash", d.getPassword());
        assertTrue(d.isEnabled());
        assertTrue(d.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void disabledUsersAreMarkedDisabled() {
        User u = TestData.user(2L, "off@test.com", "Off", false);
        u.setEnabled(false);
        when(users.findByEmail("off@test.com")).thenReturn(Optional.of(u));
        assertFalse(service.loadUserByUsername("off@test.com").isEnabled());
    }

    @Test
    void unknownUserThrows() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ghost@test.com"));
    }
}
