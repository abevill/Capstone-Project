package com.fantasyzone.service;

import com.fantasyzone.dto.UserSummary;
import com.fantasyzone.entity.*;
import com.fantasyzone.exception.*;
import com.fantasyzone.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository users;
    public AdminService(UserRepository users) { this.users = users; }

    @Transactional(readOnly = true)
    public Page<UserSummary> listUsers(Pageable pageable) {
        return users.findAll(pageable).map(this::toSummary);
    }

    @Transactional
    public UserSummary setEnabled(Long userId, boolean enabled, String currentEmail) {
        User u = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (u.getEmail().equalsIgnoreCase(currentEmail) && !enabled)
            throw new ConflictException("You cannot disable your own account");
        u.setEnabled(enabled);
        return toSummary(u);
    }

    private UserSummary toSummary(User u) {
        return new UserSummary(u.getId(), u.getEmail(), u.getDisplayName(), u.isEnabled(),
            u.getRoles().stream().map(Role::getName).sorted().toList());
    }
}
