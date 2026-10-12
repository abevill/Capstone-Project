package com.fantasyzone.controller;

import com.fantasyzone.dto.DTO;
import com.fantasyzone.service.MatchupService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MatchupController {
    private final MatchupService matchups;
    public MatchupController(MatchupService matchups) { this.matchups = matchups; }

    @GetMapping("/leagues/{leagueId}/matchups")
    public List<DTO.MatchupResponse> list(@PathVariable Long leagueId, @RequestParam(required = false) Integer week, Authentication auth) {
        return matchups.list(leagueId, week, auth.getName());
    }

    @PostMapping("/leagues/{leagueId}/matchups/generate")
    public ResponseEntity<List<DTO.MatchupResponse>> generate(@PathVariable Long leagueId, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchups.generate(leagueId, auth.getName()));
    }

    @PutMapping("/matchups/{matchupId}/calculate")
    public DTO.MatchupResponse calculate(@PathVariable Long matchupId, Authentication auth) {
        return matchups.calculate(matchupId, auth.getName());
    }
}
