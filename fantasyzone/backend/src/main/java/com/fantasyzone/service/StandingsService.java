package com.fantasyzone.service;

import com.fantasyzone.dto.DTO;
import com.fantasyzone.entity.FantasyTeam;
import com.fantasyzone.exception.NotFoundException;
import com.fantasyzone.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class StandingsService {
    private final FantasyTeamRepository teams;
    private final LeagueRepository leagues;

    public StandingsService(FantasyTeamRepository teams, LeagueRepository leagues) {
        this.teams = teams; this.leagues = leagues;
    }

    @Transactional(readOnly = true)
    public List<DTO.StandingRow> standings(Long leagueId) {
        if (!leagues.existsById(leagueId)) throw new NotFoundException("League not found");
        List<DTO.StandingRow> rows = new ArrayList<>();
        int rank = 1;
        for (FantasyTeam t : teams.findStandings(leagueId)) {
            rows.add(new DTO.StandingRow(rank++, t.getId(), t.getName(), t.getOwner().getDisplayName(),
                t.getWins(), t.getLosses(), t.getTies(), t.getPointsFor()));
        }
        return rows;
    }
}
