package com.fantasyzone.service;

import com.fantasyzone.entity.*;
import com.fantasyzone.exception.*;
import com.fantasyzone.repository.*;
import org.springframework.stereotype.Component;

/** Central place for "who is allowed to do what inside a league" rules. Admins bypass league checks. */
@Component
public class LeagueAccess {
    private final UserRepository users;
    private final LeagueMemberRepository members;

    public LeagueAccess(UserRepository users, LeagueMemberRepository members) {
        this.users = users; this.members = members;
    }

    public User user(String email) {
        return users.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
    }

    public boolean isAdmin(String email) { return isAdmin(user(email)); }

    private boolean isAdmin(User u) {
        return u.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
    }

    public void requireMember(Long leagueId, String email) {
        User u = user(email);
        if (isAdmin(u)) return;
        if (!members.existsByUserIdAndLeagueId(u.getId(), leagueId))
            throw new ForbiddenException("You are not a member of this league");
    }

    public void requireCommissioner(Long leagueId, String email) {
        User u = user(email);
        if (isAdmin(u)) return;
        LeagueMember m = members.findByUserIdAndLeagueId(u.getId(), leagueId)
            .orElseThrow(() -> new ForbiddenException("You are not a member of this league"));
        if (!m.isCommissioner()) throw new ForbiddenException("Only the commissioner can do that");
    }
}
