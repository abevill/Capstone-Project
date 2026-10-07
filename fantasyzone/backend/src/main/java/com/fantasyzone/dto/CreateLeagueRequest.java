package com.fantasyzone.dto;
import jakarta.validation.constraints.*;
public record CreateLeagueRequest(@NotBlank @Size(max = 80) String name, @Min(2000) @Max(2100) int season) {}
