package com.fantasyzone.service;

import com.fantasyzone.dto.*;
import com.fantasyzone.entity.*;
import com.fantasyzone.exception.*;
import com.fantasyzone.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TeamService {
    private final FantasyTeamRepository teams;
    private final LeagueRepository leagues;
    private final RosterPlayerRepository roster;
    private final LeagueAccess access;

    public TeamService(FantasyTeamRepository teams, LeagueRepository leagues, RosterPlayerRepository roster, LeagueAccess access) {
        this.teams = teams; this.leagues = leagues; this.roster = roster; this.access = access;
    }

    @Transactional
    public TeamResponse create(Long leagueId, TeamRequest req, String email) {
        League league = leagues.findById(leagueId).orElseThrow(() -> new NotFoundException("League not found"));
        access.requireMember(leagueId, email);
        User owner = access.user(email);
        String name = req.name().trim();
        if (teams.existsByLeagueIdAndOwnerId(leagueId, owner.getId()))
            throw new ConflictException("You already have a team in this league");
        if (teams.existsByLeagueIdAndName(leagueId, name))
            throw new ConflictException("That team name is taken in this league");
        FantasyTeam t = new FantasyTeam();
        t.setName(name); t.setLeague(league); t.setOwner(owner);
        return toResponse(teams.save(t));
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listForLeague(Long leagueId, String email) {
        access.requireMember(leagueId, email);
        return teams.findByLeagueId(leagueId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse get(Long teamId, String email) {
        FantasyTeam t = find(teamId);
        access.requireMember(t.getLeague().getId(), email);
        return toResponse(t);
    }

    @Transactional
    public TeamResponse rename(Long teamId, TeamRequest req, String email) {
        FantasyTeam t = find(teamId);
        if (!t.getOwner().getEmail().equalsIgnoreCase(email) && !access.isAdmin(email))
            throw new ForbiddenException("You can only rename your own team");
        String name = req.name().trim();
        if (!name.equals(t.getName()) && teams.existsByLeagueIdAndName(t.getLeague().getId(), name))
            throw new ConflictException("That team name is taken in this league");
        t.setName(name);
        return toResponse(t);
    }

    @Transactional(readOnly = true)
    public List<RosterEntryResponse> roster(Long teamId, String email) {
        FantasyTeam t = find(teamId);
        access.requireMember(t.getLeague().getId(), email);
        return roster.findByFantasyTeamId(teamId).stream().map(RosterService::toEntry).toList();
    }

    private FantasyTeam find(Long id) {
        return teams.findById(id).orElseThrow(() -> new NotFoundException("Team not found"));
    }

    private TeamResponse toResponse(FantasyTeam t) {
        return new TeamResponse(t.getId(), t.getName(), t.getLeague().getId(), t.getOwner().getDisplayName(),
            t.getWins(), t.getLosses(), t.getTies(), t.getPointsFor());
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> mine(String email) {
        return teams.findByOwnerId(access.user(email).getId()).stream().map(this::toResponse).toList();
    }
}
