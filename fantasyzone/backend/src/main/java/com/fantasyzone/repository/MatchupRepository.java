package com.fantasyzone.repository;

import com.fantasyzone.entity.Matchup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface MatchupRepository extends JpaRepository<Matchup, Long> {
    List<Matchup> findByLeagueIdOrderByWeek(Long leagueId);
    List<Matchup> findByLeagueIdAndWeek(Long leagueId, int week);
    boolean existsByLeagueId(Long leagueId);
    boolean existsByHomeTeamIdOrAwayTeamId(Long homeTeamId, Long awayTeamId);
}
