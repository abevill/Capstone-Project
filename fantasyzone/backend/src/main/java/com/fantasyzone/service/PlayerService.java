package com.fantasyzone.service;

import com.fantasyzone.dto.DTO;
import com.fantasyzone.entity.NflPlayer;
import com.fantasyzone.entity.PlayerWeeklyStats;
import com.fantasyzone.exception.NotFoundException;
import com.fantasyzone.repository.NflPlayerRepository;
import com.fantasyzone.repository.PlayerWeeklyStatsRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class PlayerService {

    private final NflPlayerRepository nflPlayerRepository;
    private final PlayerWeeklyStatsRepository playerWeeklyStatsRepository;
    private final FantasyScoringService fantasyScoringService;

    public PlayerService(
            NflPlayerRepository nflPlayerRepository,
            PlayerWeeklyStatsRepository playerWeeklyStatsRepository,
            FantasyScoringService fantasyScoringService) {

        this.nflPlayerRepository = nflPlayerRepository;
        this.playerWeeklyStatsRepository = playerWeeklyStatsRepository;
        this.fantasyScoringService = fantasyScoringService;
    }

    public DTO.PlayersStatsResponse getPlayerStats(Long playerId) {

        NflPlayer player = nflPlayerRepository.findById(playerId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Player " + playerId + " was not found"
                        )
                );

        List<PlayerWeeklyStats> stats =
                playerWeeklyStatsRepository.findByNflPlayerId(playerId);

            List<DTO.WeeklyStatsResponse> weeklyResponses =
                stats.stream()
                        .map(stat -> {

                            double points =
                                    fantasyScoringService
                                            .calculateFantasyPoints(stat);

                            return new DTO.WeeklyStatsResponse(
                                    stat.getSeason(),
                                    stat.getWeek(),

                                    stat.getPassingYards(),
                                    stat.getPassingTouchdowns(),
                                    stat.getInterceptions(),

                                    stat.getRushingYards(),
                                    stat.getRushingTouchdowns(),

                                    stat.getReceptions(),
                                    stat.getReceivingYards(),
                                    stat.getReceivingTouchdowns(),

                                    stat.getFumblesLost(),

                                    points
                            );
                        })
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                DTO.WeeklyStatsResponse::season)
                                        .thenComparingInt(
                                                DTO.WeeklyStatsResponse::week)
                        )
                        .toList();

        double totalPoints =
                weeklyResponses.stream()
                        .mapToDouble(
                                DTO.WeeklyStatsResponse::fantasyPoints)
                        .sum();

        return new DTO.PlayersStatsResponse(
                player.getId(),
                player.getName(),
                player.getPosition(),
                player.getNflTeam(),
                totalPoints,
                weeklyResponses
        );
    }
}