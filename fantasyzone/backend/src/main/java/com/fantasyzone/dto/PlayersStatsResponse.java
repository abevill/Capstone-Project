package com.fantasyzone.dto;

import java.util.List;

public record PlayersStatsResponse(
        Long playerId,
        String name,
        String position,
        String nflTeam,
        double totalFantasyPoints,
        List<WeeklyStatsResponse> weeklyStats
) {
}