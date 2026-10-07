package com.fantasyzone.controller;

import com.fantasyzone.dto.*;
import com.fantasyzone.entity.*;
import com.fantasyzone.exception.NotFoundException;
import com.fantasyzone.repository.*;
import com.fantasyzone.service.FantasyScoringService;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Public, read-only view of real-world NFL data. */
@RestController
@RequestMapping("/api/players")
public class PlayerController {
    private final NflPlayerRepository players;
    private final PlayerWeeklyStatsRepository stats;
    private final FantasyScoringService scoring;

    public PlayerController(NflPlayerRepository players, PlayerWeeklyStatsRepository stats, FantasyScoringService scoring) {
        this.players = players; this.stats = stats; this.scoring = scoring;
    }

    @GetMapping
    public Page<NflPlayer> list(@RequestParam(required = false) String position,
                                @RequestParam(required = false) String team,
                                @RequestParam(required = false) String name,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("name"));
        if (name != null && !name.isBlank()) return players.findByNameContainingIgnoreCase(name.trim(), p);
        if (position != null && !position.isBlank()) return players.findByPosition(position.trim().toUpperCase(), p);
        if (team != null && !team.isBlank()) return players.findByNflTeam(team.trim().toUpperCase(), p);
        return players.findAll(p);
    }

    @GetMapping("/{playerId}")
    public NflPlayer get(@PathVariable Long playerId) {
        return players.findById(playerId).orElseThrow(() -> new NotFoundException("Player not found"));
    }


    @GetMapping("/{playerId}/stats")
    public List<StatLineResponse> stats(@PathVariable Long playerId) {
        NflPlayer player = get(playerId);
        return stats.findByNflPlayerId(playerId).stream().map(s -> new StatLineResponse(
            s.getSeason(), s.getWeek(), scoring.calculateFantasyPoints(s),
            new PlayerWeeklyStatsDto(player.getExternalId(), s.getSeason(), s.getWeek(),
                s.getPassingYards(), s.getPassingTouchdowns(), s.getInterceptions(),
                s.getRushingYards(), s.getRushingTouchdowns(),
                s.getReceptions(), s.getReceivingYards(), s.getReceivingTouchdowns(), s.getFumblesLost()))).toList();
    }
}
