package com.fantasyzone.service;

import com.fantasyzone.dto.DTO;
import com.fantasyzone.dto.DTO.*;
import com.fantasyzone.entity.*;
import com.fantasyzone.exception.BadRequestException;
import com.fantasyzone.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

/**
 * Copies real-world players and weekly stats from the sports provider into our own tables.
 * The slow network calls happen first; the database is only touched in one short transaction afterwards.
 */
@Service
public class StatsSyncService {
    private static final Logger log = LoggerFactory.getLogger(StatsSyncService.class);

    private final SportsDataService sports;
    private final NflPlayerRepository players;
    private final PlayerWeeklyStatsRepository stats;
    private final TransactionTemplate tx;

    public StatsSyncService(SportsDataService sports, NflPlayerRepository players,
                            PlayerWeeklyStatsRepository stats, TransactionTemplate tx) {
        this.sports = sports; this.players = players; this.stats = stats; this.tx = tx;
    }

    public SyncResult syncPlayers() {
        List<NflPlayerDto> fetched = sports.getPlayers();
        SyncResult result = tx.execute(status -> persistPlayers(fetched));
        log.info("Player sync: {}", result);
        return result;
    }

    /** Season and week are optional; when either is missing the provider's current week is used. */
    public SyncResult syncStats(Integer season, Integer week) {
        SeasonWeek target = (season == null || week == null) ? sports.currentWeek() : new SeasonWeek(season, week);
        if (target.season() < 2000 || target.season() > 2100) throw new BadRequestException("Season must be between 2000 and 2100");
        if (target.week() < 1 || target.week() > 18) throw new BadRequestException("Week must be between 1 and 18");

        List<PlayerWeeklyStatsDto> fetched = sports.getWeeklyStats(target.season(), target.week());
        SyncResult result = tx.execute(status -> persistStats(fetched, target));
        log.info("Stats sync for {} week {}: {}", target.season(), target.week(), result);
        return result;
    }

    private SyncResult persistPlayers(List<NflPlayerDto> fetched) {
        Map<String, NflPlayer> existing = new HashMap<>();
        for (NflPlayer p : players.findAll()) if (p.getExternalId() != null) existing.put(p.getExternalId(), p);

        List<NflPlayer> toCreate = new ArrayList<>();
        int updated = 0;
        for (NflPlayerDto d : fetched) {
            NflPlayer p = existing.get(d.externalId());
            if (p == null) {
                p = new NflPlayer();
                p.setExternalId(d.externalId());
                p.setName(d.name());
                p.setPosition(d.position());
                p.setNflTeam(d.nflTeam());
                toCreate.add(p);
            } else if (!Objects.equals(p.getName(), d.name()) || !Objects.equals(p.getPosition(), d.position())
                    || !Objects.equals(p.getNflTeam(), d.nflTeam())) {
                p.setName(d.name());
                p.setPosition(d.position());
                p.setNflTeam(d.nflTeam());
                updated++; // managed entity: saved when the transaction commits
            }
        }
        players.saveAll(toCreate);
        return SyncResult.players(toCreate.size(), updated);
    }

    private SyncResult persistStats(List<PlayerWeeklyStatsDto> fetched, SeasonWeek target) {
        Map<String, NflPlayer> byExternalId = new HashMap<>();
        for (NflPlayer p : players.findAll()) if (p.getExternalId() != null) byExternalId.put(p.getExternalId(), p);

        int created = 0, updated = 0, skipped = 0;
        for (PlayerWeeklyStatsDto d : fetched) {
            NflPlayer player = byExternalId.get(d.externalPlayerId());
            if (player == null) { skipped++; continue; } // not a synced QB/RB/WR/TE (defense, linemen, new signing)

            Optional<PlayerWeeklyStats> found = stats.findByNflPlayerIdAndSeasonAndWeek(player.getId(), target.season(), target.week());
            PlayerWeeklyStats line = found.orElseGet(PlayerWeeklyStats::new);
            boolean isNew = found.isEmpty();
            boolean changed = isNew || differs(line, d);
            if (!changed) continue;

            line.setNflPlayer(player);
            line.setSeason(target.season());
            line.setWeek(target.week());
            line.setPassingYards(d.passingYards());
            line.setPassingTouchdowns(d.passingTouchdowns());
            line.setInterceptions(d.interceptions());
            line.setRushingYards(d.rushingYards());
            line.setRushingTouchdowns(d.rushingTouchdowns());
            line.setReceptions(d.receptions());
            line.setReceivingYards(d.receivingYards());
            line.setReceivingTouchdowns(d.receivingTouchdowns());
            line.setFumblesLost(d.fumblesLost());
            stats.save(line);
            if (isNew) created++; else updated++;
        }
        return SyncResult.stats(created, updated, skipped);
    }

    private static boolean differs(PlayerWeeklyStats l, PlayerWeeklyStatsDto d) {
        return l.getPassingYards() != d.passingYards() || l.getPassingTouchdowns() != d.passingTouchdowns()
            || l.getInterceptions() != d.interceptions() || l.getRushingYards() != d.rushingYards()
            || l.getRushingTouchdowns() != d.rushingTouchdowns() || l.getReceptions() != d.receptions()
            || l.getReceivingYards() != d.receivingYards() || l.getReceivingTouchdowns() != d.receivingTouchdowns()
            || l.getFumblesLost() != d.fumblesLost();
    }
}
