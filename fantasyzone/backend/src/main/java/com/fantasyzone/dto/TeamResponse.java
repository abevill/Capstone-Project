package com.fantasyzone.dto;
public record TeamResponse(Long id, String name, Long leagueId, String owner, int wins, int losses, int ties, double pointsFor) {}
