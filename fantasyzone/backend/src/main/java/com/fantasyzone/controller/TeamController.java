package com.fantasyzone.controller;

import com.fantasyzone.dto.*;
import com.fantasyzone.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TeamController {
    private final TeamService teams;
    private final RosterService rosters;
    private final TransactionService transactionService;

    public TeamController(TeamService teams, RosterService rosters, TransactionService transactionService) {
        this.teams = teams; this.rosters = rosters; this.transactionService = transactionService;
    }

    @GetMapping("/leagues/{leagueId}/teams")
    public List<TeamResponse> list(@PathVariable Long leagueId, Authentication auth) {
        return teams.listForLeague(leagueId, auth.getName());
    }

    @PostMapping("/leagues/{leagueId}/teams")
    public ResponseEntity<TeamResponse> create(@PathVariable Long leagueId, @Valid @RequestBody TeamRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teams.create(leagueId, req, auth.getName()));
    }

    @GetMapping("/teams/{teamId}")
    public TeamResponse get(@PathVariable Long teamId, Authentication auth) { return teams.get(teamId, auth.getName()); }

    @PutMapping("/teams/{teamId}")
    public TeamResponse rename(@PathVariable Long teamId, @Valid @RequestBody TeamRequest req, Authentication auth) {
        return teams.rename(teamId, req, auth.getName());
    }

    @GetMapping("/teams/{teamId}/roster")
    public List<RosterEntryResponse> roster(@PathVariable Long teamId, Authentication auth) {
        return teams.roster(teamId, auth.getName());
    }

    @PostMapping("/teams/{teamId}/roster/players/{playerId}")
    public ResponseEntity<RosterEntryResponse> add(@PathVariable Long teamId, @PathVariable Long playerId,
                                                   @Valid @RequestBody AddPlayerRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(rosters.addPlayer(teamId, playerId, req.positionSlot(), auth.getName()));
    }

    @DeleteMapping("/teams/{teamId}/roster/players/{playerId}")
    public ResponseEntity<Void> drop(@PathVariable Long teamId, @PathVariable Long playerId, Authentication auth) {
        rosters.dropPlayer(teamId, playerId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teams/{teamId}/transactions")
    public List<TransactionResponse> transactions(@PathVariable Long teamId, Authentication auth) {
        return transactionService.history(teamId, auth.getName());
    }
}
