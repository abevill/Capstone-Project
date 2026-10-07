package com.fantasyzone.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeagueResponse(Long id, String name, int season, boolean rostersLocked, long memberCount, String joinCode) {}
