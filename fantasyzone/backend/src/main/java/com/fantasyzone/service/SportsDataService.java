package com.fantasyzone.service;

import com.fantasyzone.dto.*;
import java.util.List;

/** Boundary to the external provider. It supplies real-world data only; it never runs the league. */
public interface SportsDataService {
    /** Fantasy-relevant players (QB, RB, WR, TE) currently on NFL rosters. */
    List<NflPlayerDto> getPlayers();

    /** One stat line per player who recorded offensive stats in the finished games of a regular-season week. */
    List<PlayerWeeklyStatsDto> getWeeklyStats(int season, int week);

    /** The season and week the provider currently reports. */
    SeasonWeek currentWeek();
}
