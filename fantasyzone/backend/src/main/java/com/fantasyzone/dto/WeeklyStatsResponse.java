package com.fantasyzone.dto;

public record WeeklyStatsResponse(
        int season,
        int week,

        int passingYards,
        int passingTouchdowns,
        int interceptions,

        int rushingYards,
        int rushingTouchdowns,

        int receptions,
        int receivingYards,
        int receivingTouchdowns,

        int fumblesLost,

        double fantasyPoints
) {
}