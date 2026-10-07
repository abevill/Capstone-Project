package com.fantasyzone.service;

import com.fantasyzone.dto.*;
import com.fantasyzone.entity.*;
import com.fantasyzone.exception.*;
import com.fantasyzone.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class LeagueService {
    private final LeagueRepository leagues;
    private final LeagueMemberRepository members;
    private final FantasyTeamRepository teams;
    private final MatchupRepository matchups;
    private final LeagueAccess access;

    public LeagueService(LeagueRepository leagues, LeagueMemberRepository members, FantasyTeamRepository teams,
                         MatchupRepository matchups, LeagueAccess access) {
        this.leagues = leagues; this.members = members; this.teams = teams; this.matchups = matchups; this.access = access;
    }

    @Transactional
    public LeagueResponse create(CreateLeagueRequest req, String email) {
        User user = access.user(email);
        League l = new League();
        l.setName(req.name().trim());
        l.setSeason(req.season());
        l.setJoinCode(newJoinCode());
        leagues.save(l);
        addMember(l, user, true);
        return toResponse(l, true);
    }

    @Transactional
    public LeagueResponse join(String joinCode, String email) {
        User user = access.user(email);
        League l = leagues.findByJoinCode(joinCode.trim().toUpperCase())
            .orElseThrow(() -> new NotFoundException("Invalid join code"));
        if (members.existsByUserIdAndLeagueId(user.getId(), l.getId()))
            throw new ConflictException("You are already in this league");
        addMember(l, user, false);
        return toResponse(l, false);
    }

    @Transactional(readOnly = true)
    public Page<LeagueResponse> list(Pageable pageable) {
        return leagues.findAll(pageable).map(l -> toResponse(l, false));
    }

    @Transactional(readOnly = true)
    public LeagueResponse get(Long id) {
        return toResponse(find(id), false);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> members(Long leagueId, String email) {
        find(leagueId);
        access.requireMember(leagueId, email);
        return members.findByLeagueId(leagueId).stream()
            .map(m -> new MemberResponse(m.getUser().getId(), m.getUser().getDisplayName(), m.isCommissioner()))
            .toList();
    }

    @Transactional
    public void removeMember(Long leagueId, Long userId, String email) {
        find(leagueId);
        access.requireCommissioner(leagueId, email);
        LeagueMember target = members.findByUserIdAndLeagueId(userId, leagueId)
            .orElseThrow(() -> new NotFoundException("Member not found in this league"));
        if (target.isCommissioner()) throw new ConflictException("The commissioner cannot be removed");
        teams.findByLeagueIdAndOwnerId(leagueId, userId).ifPresent(t -> {
            if (matchups.existsByHomeTeamIdOrAwayTeamId(t.getId(), t.getId()))
                throw new ConflictException("Cannot remove a member whose team already has matchups");
            teams.delete(t);
        });
        members.delete(target);
    }

    @Transactional
    public LeagueResponse setRosterLock(Long leagueId, boolean locked, String email) {
        League l = find(leagueId);
        access.requireCommissioner(leagueId, email);
        l.setRostersLocked(locked);
        return toResponse(l, false);
    }

    private League find(Long id) {
        return leagues.findById(id).orElseThrow(() -> new NotFoundException("League not found"));
    }

    private void addMember(League l, User u, boolean commissioner) {
        LeagueMember m = new LeagueMember();
        m.setLeague(l); m.setUser(u); m.setCommissioner(commissioner);
        members.save(m);
    }

    private String newJoinCode() {
        String code;
        do { code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(); }
        while (leagues.findByJoinCode(code).isPresent());
        return code;
    }

    private LeagueResponse toResponse(League l, boolean includeCode) {
        return new LeagueResponse(l.getId(), l.getName(), l.getSeason(), l.isRostersLocked(),
            members.countByLeagueId(l.getId()), includeCode ? l.getJoinCode() : null);
    }

    /** Leagues the caller belongs to; the join code is included only for leagues they commission. */
    @Transactional(readOnly = true)
    public List<LeagueResponse> mine(String email) {
        User u = access.user(email);
        return members.findByUserId(u.getId()).stream()
            .map(m -> toResponse(m.getLeague(), m.isCommissioner())).toList();
    }
}
