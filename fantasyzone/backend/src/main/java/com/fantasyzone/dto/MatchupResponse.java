package com.fantasyzone.dto;
public record MatchupResponse(Long id, int week, Long homeTeamId, String homeTeam, Long awayTeamId, String awayTeam,
                              Double homeScore, Double awayScore, boolean finalized) {}
