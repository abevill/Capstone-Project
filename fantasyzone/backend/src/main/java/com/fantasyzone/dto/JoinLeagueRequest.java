package com.fantasyzone.dto;
import jakarta.validation.constraints.NotBlank;
public record JoinLeagueRequest(@NotBlank String joinCode) {}
