package com.fantasyzone.service;

import com.fantasyzone.dto.DTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Optional automatic sync. Off by default; turn on with SYNC_ENABLED=true. */
@Component
@ConditionalOnProperty(name = "sync.enabled", havingValue = "true")
public class ScheduledSync {

    private static final Logger log =
            LoggerFactory.getLogger(ScheduledSync.class);

    private final StatsSyncService sync;
    private final SportsDataService sports;

    public ScheduledSync(
            StatsSyncService sync,
            SportsDataService sports
    ) {
        this.sync = sync;
        this.sports = sports;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncPlayersOnStartup() {
        try {
            log.info("Starting ESPN player sync...");
            sync.syncPlayers();
            log.info("ESPN player sync completed.");
        } catch (RuntimeException e) {
            log.warn("Startup player sync failed: {}", e.getMessage());
        }
    }

    /** Daily: refresh rosters (signings, trades, position changes). */
    @Scheduled(cron = "${sync.players-cron:0 30 5 * * *}")
    public void syncPlayers() {
        try {
            sync.syncPlayers();
        } catch (RuntimeException e) {
            log.warn("Scheduled player sync failed: {}", e.getMessage());
        }
    }

    /** Every 6 hours: current week plus previous week to pick up stat corrections. */
    @Scheduled(cron = "${sync.stats-cron:0 0 */6 * * *}")
    public void syncStats() {
        try {
            DTO.SeasonWeek now = sports.currentWeek();

            sync.syncStats(now.season(), now.week());

            if (now.week() > 1) {
                sync.syncStats(now.season(), now.week() - 1);
            }
        } catch (RuntimeException e) {
            log.warn("Scheduled stats sync failed: {}", e.getMessage());
        }
    }
}


