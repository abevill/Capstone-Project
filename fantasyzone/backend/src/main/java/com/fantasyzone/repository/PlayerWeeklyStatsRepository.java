package com.fantasyzone.repository;

import com.fantasyzone.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface PlayerWeeklyStatsRepository extends JpaRepository<PlayerWeeklyStats, Long> {
    Optional<PlayerWeeklyStats> findByNflPlayerIdAndSeasonAndWeek(Long playerId, int season, int week);
    List<PlayerWeeklyStats> findByNflPlayerId(Long playerId);
}
