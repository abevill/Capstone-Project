package com.fantasyzone.dto;
public record StandingRow(int rank, Long teamId, String teamName, String owner, int wins, int losses, int ties, double pointsFor) {}
