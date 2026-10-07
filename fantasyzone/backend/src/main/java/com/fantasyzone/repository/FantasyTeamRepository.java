package com.fantasyzone.repository;

import com.fantasyzone.entity.FantasyTeam;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface FantasyTeamRepository extends JpaRepository<FantasyTeam, Long> {
    List<FantasyTeam> findByLeagueId(Long leagueId);
    Optional<FantasyTeam> findByLeagueIdAndOwnerId(Long leagueId, Long ownerId);
    boolean existsByLeagueIdAndOwnerId(Long leagueId, Long ownerId);
    boolean existsByLeagueIdAndName(Long leagueId, String name);

    @Query("select t from FantasyTeam t where t.league.id = :leagueId order by t.wins desc, t.pointsFor desc")
    List<FantasyTeam> findStandings(@Param("leagueId") Long leagueId);
    List<FantasyTeam> findByOwnerId(Long ownerId);
}
