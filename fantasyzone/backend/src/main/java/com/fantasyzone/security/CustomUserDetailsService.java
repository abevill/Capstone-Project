package com.fantasyzone.security;

import com.fantasyzone.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public CustomUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.fantasyzone.entity.User u = users.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User.withUsername(u.getEmail())
            .password(u.getPasswordHash())
            .authorities(u.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName())).toList())
            .disabled(!u.isEnabled())
            .build();
    }
}
