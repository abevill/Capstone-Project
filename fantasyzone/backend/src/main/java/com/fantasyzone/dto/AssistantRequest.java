package com.fantasyzone.dto;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * A question for the AI assistant.
 * teamId (optional): include that team's roster, record, standings, and matchup; the caller must be in the team's league.
 * playerIds (optional, up to 6): include those players' recent stats (for "compare these players").
 * week (optional): the week to look at for the matchup; defaults to the team's next unscored matchup.
 */
public record AssistantRequest(
    @NotBlank @Size(max = 500) String question,
    Long teamId,
    @Size(max = 6) List<Long> playerIds,
    @Min(1) @Max(18) Integer week) {}
