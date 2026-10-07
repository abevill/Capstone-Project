package com.fantasyzone.repository;

import com.fantasyzone.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface RosterPlayerRepository extends JpaRepository<RosterPlayer, Long> {
    List<RosterPlayer> findByFantasyTeamId(Long teamId);
    Optional<RosterPlayer> findByFantasyTeamIdAndNflPlayerId(Long teamId, Long playerId);

    @Query("select count(r) > 0 from RosterPlayer r where r.fantasyTeam.league.id = :leagueId and r.nflPlayer.id = :playerId")
    boolean isPlayerTakenInLeague(@Param("leagueId") Long leagueId, @Param("playerId") Long playerId);
}
