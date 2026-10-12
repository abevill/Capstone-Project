package com.fantasyzone.controller;

import com.fantasyzone.dto.DTO;
import com.fantasyzone.dto.DTO.*;
import com.fantasyzone.service.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Secured to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin;
    private final StatsSyncService sync;

    public AdminController(AdminService admin, StatsSyncService sync) { this.admin = admin; this.sync = sync; }

    @GetMapping("/users")
    public Page<UserSummary> users(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return admin.listUsers(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("email")));
    }

    @PutMapping("/users/{userId}/status")
    public UserSummary status(@PathVariable Long userId, @RequestParam boolean enabled, Authentication auth) {
        return admin.setEnabled(userId, enabled, auth.getName());
    }

    /** Pulls QB/RB/WR/TE rosters for all teams from the sports provider (takes about half a minute). */
    @PostMapping("/players/sync")
    public SyncResult syncPlayers() { return sync.syncPlayers(); }

    /** Pulls box scores for a week's finished games. Without season and week, the current week is used. */
    @PostMapping("/stats/sync")
    public SyncResult syncStats(@RequestParam(required = false) Integer season, @RequestParam(required = false) Integer week) {
        return sync.syncStats(season, week);
    }
}
