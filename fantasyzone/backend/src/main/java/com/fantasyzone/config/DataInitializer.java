package com.fantasyzone.config;

import com.fantasyzone.entity.*;
import com.fantasyzone.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates roles, an admin account, and SAMPLE players/stats so the demo works without the external API. */
@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final int SAMPLE_SEASON = 2025;

    private final RoleRepository roles;
    private final UserRepository users;
    private final NflPlayerRepository players;
    private final PlayerWeeklyStatsRepository stats;
    private final PasswordEncoder encoder;
    private final String adminPassword;
    private final boolean seedSamples;

    public DataInitializer(RoleRepository roles, UserRepository users, NflPlayerRepository players,
                           PlayerWeeklyStatsRepository stats, PasswordEncoder encoder,
                           @Value("${app.admin-password}") String adminPassword,
                           @Value("${app.seed-sample-players:true}") boolean seedSamples) {
        this.seedSamples = seedSamples;
        this.roles = roles; this.users = users; this.players = players; this.stats = stats;
        this.encoder = encoder; this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        Role userRole = role("ROLE_USER");
        Role adminRole = role("ROLE_ADMIN");

        if (!users.existsByEmail("admin@fantasyzone.test")) {
            User admin = new User();
            admin.setEmail("admin@fantasyzone.test");
            admin.setDisplayName("Admin");
            admin.setPasswordHash(encoder.encode(adminPassword));
            admin.getRoles().add(userRole);
            admin.getRoles().add(adminRole);
            users.save(admin);
            log.warn("Seeded admin@fantasyzone.test. Set ADMIN_PASSWORD in any shared environment.");
        }

        if (seedSamples && players.count() == 0) seedPlayers();
    }

    private Role role(String name) {
        return roles.findByName(name).orElseGet(() -> {
            Role r = new Role();
            r.setName(name);
            return roles.save(r);
        });
    }

    // name, position, team, passYds, passTd, int, rushYds, rushTd, rec, recYds, recTd, fumLost  (SAMPLE numbers, not real stats)
    private void seedPlayers() {
        Object[][] data = {
            {"Josh Allen", "QB", "BUF", 280, 2, 1, 45, 1, 0, 0, 0, 0},
            {"Patrick Mahomes", "QB", "KC", 301, 3, 0, 12, 0, 0, 0, 0, 0},
            {"Lamar Jackson", "QB", "BAL", 245, 2, 0, 85, 1, 0, 0, 0, 0},
            {"Christian McCaffrey", "RB", "SF", 0, 0, 0, 95, 1, 5, 40, 0, 0},
            {"Derrick Henry", "RB", "BAL", 0, 0, 0, 110, 1, 1, 8, 0, 0},
            {"Saquon Barkley", "RB", "PHI", 0, 0, 0, 85, 0, 4, 30, 0, 0},
            {"Justin Jefferson", "WR", "MIN", 0, 0, 0, 0, 0, 8, 120, 1, 0},
            {"Ja'Marr Chase", "WR", "CIN", 0, 0, 0, 0, 0, 7, 95, 1, 0},
            {"CeeDee Lamb", "WR", "DAL", 0, 0, 0, 0, 0, 6, 80, 0, 0},
            {"Travis Kelce", "TE", "KC", 0, 0, 0, 0, 0, 7, 70, 1, 0},
            {"George Kittle", "TE", "SF", 0, 0, 0, 0, 0, 5, 60, 0, 0},
            {"Sam LaPorta", "TE", "DET", 0, 0, 0, 0, 0, 4, 45, 0, 1},
        };
        int i = 1;
        for (Object[] d : data) {
            NflPlayer p = new NflPlayer();
            p.setExternalId("seed-" + i++);
            p.setName((String) d[0]);
            p.setPosition((String) d[1]);
            p.setNflTeam((String) d[2]);
            players.save(p);

            PlayerWeeklyStats s = new PlayerWeeklyStats();
            s.setNflPlayer(p);
            s.setSeason(SAMPLE_SEASON);
            s.setWeek(1);
            s.setPassingYards((int) d[3]); s.setPassingTouchdowns((int) d[4]); s.setInterceptions((int) d[5]);
            s.setRushingYards((int) d[6]); s.setRushingTouchdowns((int) d[7]);
            s.setReceptions((int) d[8]); s.setReceivingYards((int) d[9]); s.setReceivingTouchdowns((int) d[10]);
            s.setFumblesLost((int) d[11]);
            stats.save(s);
        }
        log.info("Seeded {} sample players with week 1 stats for season {}", data.length, SAMPLE_SEASON);
    }
}
