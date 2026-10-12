package com.fantasyzone.controller;

import com.fantasyzone.dto.DTO;
import com.fantasyzone.dto.DTO.*;
import com.fantasyzone.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/leagues")
public class LeagueController {
    private final LeagueService leagues;
    private final StandingsService standings;

    public LeagueController(LeagueService leagues, StandingsService standings) {
        this.leagues = leagues; this.standings = standings;
    }

    // ---- public ----
    @GetMapping
    public Page<LeagueResponse> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return leagues.list(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("name")));
    }

    @GetMapping("/{leagueId}")
    public LeagueResponse get(@PathVariable Long leagueId) { return leagues.get(leagueId); }

    @GetMapping("/{leagueId}/standings")
    public List<StandingRow> standings(@PathVariable Long leagueId) { return standings.standings(leagueId); }

    // ---- authenticated ----
    @PostMapping
    public ResponseEntity<LeagueResponse> create(@Valid @RequestBody CreateLeagueRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leagues.create(req, auth.getName()));
    }

    @PostMapping("/join")
    public LeagueResponse join(@Valid @RequestBody JoinLeagueRequest req, Authentication auth) {
        return leagues.join(req.joinCode(), auth.getName());
    }

    @GetMapping("/{leagueId}/members")
    public List<MemberResponse> members(@PathVariable Long leagueId, Authentication auth) {
        return leagues.members(leagueId, auth.getName());
    }

    // ---- commissioner ----
    @DeleteMapping("/{leagueId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long leagueId, @PathVariable Long userId, Authentication auth) {
        leagues.removeMember(leagueId, userId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{leagueId}/lock")
    public LeagueResponse lock(@PathVariable Long leagueId, @RequestParam boolean locked, Authentication auth) {
        return leagues.setRosterLock(leagueId, locked, auth.getName());
    }
}
