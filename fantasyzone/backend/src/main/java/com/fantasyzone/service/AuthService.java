package com.fantasyzone.service;

import com.fantasyzone.dto.DTO;
import com.fantasyzone.dto.DTO.*;
import com.fantasyzone.entity.*;
import com.fantasyzone.exception.*;
import com.fantasyzone.repository.*;
import com.fantasyzone.security.JwtService;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AuthService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    public AuthService(UserRepository users, RoleRepository roles, PasswordEncoder encoder,
                       AuthenticationManager authManager, JwtService jwt) {
        this.users = users; this.roles = roles; this.encoder = encoder; this.authManager = authManager; this.jwt = jwt;
    }

    @Transactional
    public DTO.AuthResponse register(DTO.RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new ConflictException("An account with that email already exists");
        User u = new User();
        u.setEmail(email);
        u.setDisplayName(req.displayName().trim());
        u.setPasswordHash(encoder.encode(req.password()));
        u.getRoles().add(roles.findByName("ROLE_USER")
            .orElseThrow(() -> new IllegalStateException("ROLE_USER is missing; run seed data")));
        users.save(u);
        return toResponse(u);
    }

    @Transactional(readOnly = true)
    public DTO.AuthResponse login(DTO.LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        authManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        return toResponse(users.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found")));
    }

    private DTO.AuthResponse toResponse(User u) {
        List<String> roleNames = u.getRoles().stream().map(Role::getName).sorted().toList();
        return new DTO.AuthResponse(u.getId(), jwt.generateToken(u.getEmail()), u.getEmail(), u.getDisplayName(), roleNames);
    }
}
