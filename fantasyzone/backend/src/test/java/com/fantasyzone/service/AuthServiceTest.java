package com.fantasyzone.service;

import com.fantasyzone.TestData;
import com.fantasyzone.dto.DTO;
import com.fantasyzone.dto.DTO.*;
import com.fantasyzone.entity.*;
import com.fantasyzone.exception.ConflictException;
import com.fantasyzone.repository.*;
import com.fantasyzone.security.JwtService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock PasswordEncoder encoder;
    @Mock AuthenticationManager authManager;
    @Mock JwtService jwt;
    AuthService service;

    @BeforeEach
    void setUp() { service = new AuthService(users, roles, encoder, authManager, jwt); }

    @Test
    void registerCreatesUserWithHashedPasswordAndUserRole() {
        Role role = new Role();
        role.setName("ROLE_USER");
        when(roles.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(encoder.encode("Password123!")).thenReturn("hashed");
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwt.generateToken("new@test.com")).thenReturn("tok");

        AuthResponse res = service.register(new RegisterRequest(" New@Test.com ", " Newbie ", "Password123!"));

        assertEquals("tok", res.token());
        assertEquals("new@test.com", res.email());
        assertEquals("Newbie", res.displayName());
        assertEquals(List.of("ROLE_USER"), res.roles());
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertEquals("hashed", saved.getValue().getPasswordHash());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(users.existsByEmail("dup@test.com")).thenReturn(true);
        assertThrows(ConflictException.class,
            () -> service.register(new RegisterRequest("dup@test.com", "Dupe", "Password123!")));
        verify(users, never()).save(any());
    }

    @Test
    void registerFailsClearlyWhenRolesAreNotSeeded() {
        when(roles.findByName("ROLE_USER")).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class,
            () -> service.register(new RegisterRequest("a@test.com", "Alice", "Password123!")));
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        when(users.findByEmail("a@test.com")).thenReturn(Optional.of(TestData.user(1L, "a@test.com", "Alice", false)));
        when(jwt.generateToken("a@test.com")).thenReturn("tok");
        AuthResponse res = service.login(new LoginRequest("A@test.com", "Password123!"));
        assertEquals("tok", res.token());
        assertEquals(1L, res.userId());
    }

    @Test
    void loginPropagatesBadCredentials() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        assertThrows(BadCredentialsException.class, () -> service.login(new LoginRequest("a@test.com", "wrong")));
    }
}
