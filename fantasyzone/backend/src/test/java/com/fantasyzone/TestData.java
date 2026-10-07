package com.fantasyzone;

import com.fantasyzone.entity.*;

/** Small factory methods so unit tests stay readable. */
public final class TestData {
    private TestData() {}

    public static User user(Long id, String email, String displayName, boolean admin) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setDisplayName(displayName);
        u.setPasswordHash("hash");
        Role user = new Role();
        user.setName("ROLE_USER");
        u.getRoles().add(user);
        if (admin) {
            Role a = new Role();
            a.setName("ROLE_ADMIN");
            u.getRoles().add(a);
        }
        return u;
    }

    public static League league(Long id, int season) {
        League l = new League();
        l.setId(id);
        l.setName("League " + id);
        l.setSeason(season);
        l.setJoinCode("CODE" + id);
        return l;
    }

    public static FantasyTeam team(Long id, String name, League league, User owner) {
        FantasyTeam t = new FantasyTeam();
        t.setId(id);
        t.setName(name);
        t.setLeague(league);
        t.setOwner(owner);
        return t;
    }

    public static NflPlayer player(Long id, String name, String position, String nflTeam) {
        NflPlayer p = new NflPlayer();
        p.setId(id);
        p.setName(name);
        p.setPosition(position);
        p.setNflTeam(nflTeam);
        return p;
    }

    public static RosterPlayer spot(FantasyTeam team, NflPlayer player, String slot) {
        RosterPlayer r = new RosterPlayer();
        r.setFantasyTeam(team);
        r.setNflPlayer(player);
        r.setPositionSlot(slot);
        return r;
    }
}
