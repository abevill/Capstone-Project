package com.fantasyzone.dto;

public record PlayerWeeklyStatsDto(String externalPlayerId, int season, int week,
    int passingYards, int passingTouchdowns, int interceptions,
    int rushingYards, int rushingTouchdowns,
    int receptions, int receivingYards, int receivingTouchdowns, int fumblesLost) {}
