package com.fantasyzone.controller;

import com.fantasyzone.dto.DTO;
import com.fantasyzone.dto.DTO.*;
import com.fantasyzone.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** The signed-in user's own leagues and teams (powers the dashboard). */
@RestController
@RequestMapping("/api/me")
public class MeController {
    private final LeagueService leagues;
    private final TeamService teams;

    public MeController(LeagueService leagues, TeamService teams) { this.leagues = leagues; this.teams = teams; }

    @GetMapping("/leagues")
    public List<LeagueResponse> leagues(Authentication auth) { return leagues.mine(auth.getName()); }

    @GetMapping("/teams")
    public List<TeamResponse> teams(Authentication auth) { return teams.mine(auth.getName()); }
}
