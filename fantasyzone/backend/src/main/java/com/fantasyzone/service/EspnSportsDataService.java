package com.fantasyzone.service;

import com.fantasyzone.dto.DTO;
import com.fantasyzone.dto.DTO.*;
import com.fantasyzone.exception.SportsDataException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Reads ESPN's public (unofficial, undocumented) NFL endpoints. No API key is needed.
 * Responses are parsed defensively; anything unexpected becomes a SportsDataException, and the rest of the
 * application keeps working from the data already stored in the database.
 */
@Service
public class EspnSportsDataService implements SportsDataService {
    private static final Logger log = LoggerFactory.getLogger(EspnSportsDataService.class);

    /** ESPN's NFL team ids (there are no ids 31 and 32). */
    private static final List<Integer> TEAM_IDS =
        Stream.concat(IntStream.rangeClosed(1, 30).boxed(), Stream.of(33, 34)).toList();

    /** Positions the fixed scoring formula can score. */
    private static final Set<String> FANTASY_POSITIONS = Set.of("QB", "RB", "WR", "TE", "PK");

    /** Box score categories that feed the scoring formula. ("interceptions" there means defensive picks, so it is ignored.) */
    private static final Set<String> SCORING_CATEGORIES = Set.of("passing", "rushing", "receiving", "fumbles");

    private final RestClient client;

    public EspnSportsDataService(@Qualifier("espnRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    public List<NflPlayerDto> getPlayers() {
        Map<String, NflPlayerDto> byId = new LinkedHashMap<>();
        int failures = 0;
        for (int teamId : TEAM_IDS) {
            try {
                JsonNode roster = get(u -> u.path("/teams/{id}/roster").build(teamId), "the roster of team " + teamId);
                for (NflPlayerDto p : parseRoster(roster)) byId.putIfAbsent(p.externalId(), p);
            } catch (SportsDataException e) {
                failures++;
                log.warn("Skipping team {}: {}", teamId, e.getMessage());
            }
        }
        if (byId.isEmpty()) throw new SportsDataException("ESPN returned no players (" + failures + " requests failed)", null);
        if (failures > 0) log.warn("Player sync finished with {} failed team requests", failures);
        return new ArrayList<>(byId.values());
    }

    @Override
    public List<PlayerWeeklyStatsDto> getWeeklyStats(int season, int week) {
        JsonNode board = get(u -> u.path("/scoreboard")
            .queryParam("dates", season).queryParam("seasontype", 2).queryParam("week", week).build(), "the scoreboard");

        Map<String, Line> lines = new LinkedHashMap<>();
        for (JsonNode event : board.path("events")) {
            if (!event.path("status").path("type").path("completed").asBoolean(false)) continue; // only finished games
            String eventId = event.path("id").asText("");
            if (eventId.isBlank()) continue;
            JsonNode summary = get(u -> u.path("/summary").queryParam("event", eventId).build(), "the box score of game " + eventId);
            collectBoxScore(summary, lines);
        }

        List<PlayerWeeklyStatsDto> out = new ArrayList<>();
        lines.forEach((id, l) -> {
            if (!l.isEmpty()) out.add(l.toDto(id, season, week));
        });
        return out;
    }

    @Override
    public SeasonWeek currentWeek() {
        JsonNode board = get(u -> u.path("/scoreboard").build(), "the scoreboard");
        int season = board.path("leagues").path(0).path("season").path("year").asInt(0);
        int week = board.path("week").path("number").asInt(0);
        if (season == 0 || week == 0) throw new SportsDataException("ESPN did not report a current season and week", null);
        return new SeasonWeek(season, week);
    }

    // ---------- parsing (package-private for tests) ----------
    static List<NflPlayerDto> parseRoster(JsonNode root) {
        String team = root.path("team").path("abbreviation").asText(null);
        List<NflPlayerDto> out = new ArrayList<>();
        for (JsonNode group : root.path("athletes")) {
            for (JsonNode a : group.path("items")) {
                String position = a.path("position").path("abbreviation").asText("");
                String id = a.path("id").asText("");
                String name = a.path("fullName").asText("");
                if (!FANTASY_POSITIONS.contains(position) || id.isBlank() || name.isBlank()) continue;
                out.add(new NflPlayerDto(id, name, position, team));
            }
        }
        return out;
    }

    private static void collectBoxScore(JsonNode summary, Map<String, Line> lines) {
        for (JsonNode team : summary.path("boxscore").path("players")) {
            for (JsonNode category : team.path("statistics")) {
                String name = category.path("name").asText("");
                if (!SCORING_CATEGORIES.contains(name)) continue;
                JsonNode keys = category.path("keys");
                for (JsonNode athlete : category.path("athletes")) {
                    String id = athlete.path("athlete").path("id").asText("");
                    if (id.isBlank()) continue;
                    Line line = lines.computeIfAbsent(id, k -> new Line());
                    JsonNode values = athlete.path("stats");
                    for (int i = 0; i < keys.size() && i < values.size(); i++) {
                        line.apply(name, keys.get(i).asText(), toInt(values.get(i)));
                    }
                }
            }
        }
    }

    private static int toInt(JsonNode value) {
        try { return Integer.parseInt(value.asText().trim()); }
        catch (NumberFormatException e) { return 0; } // "--" and similar placeholders
    }

    private JsonNode get(Function<UriBuilder, URI> uri, String what) {
        try {
            JsonNode body = client.get().uri(uri).retrieve().body(JsonNode.class);
            if (body == null) throw new SportsDataException("ESPN returned an empty response for " + what, null);
            return body;
        } catch (RestClientException e) {
            throw new SportsDataException("Could not get " + what + " from ESPN", e);
        }
    }

    /** Running totals for one player across the box score categories. */
    private static final class Line {
        int passingYards, passingTouchdowns, interceptions, rushingYards, rushingTouchdowns,
            receptions, receivingYards, receivingTouchdowns, fumblesLost;

        void apply(String category, String key, int v) {
            switch (category + ":" + key) {
                case "passing:passingYards" -> passingYards += v;
                case "passing:passingTouchdowns" -> passingTouchdowns += v;
                case "passing:interceptions" -> interceptions += v;
                case "rushing:rushingYards" -> rushingYards += v;
                case "rushing:rushingTouchdowns" -> rushingTouchdowns += v;
                case "receiving:receptions" -> receptions += v;
                case "receiving:receivingYards" -> receivingYards += v;
                case "receiving:receivingTouchdowns" -> receivingTouchdowns += v;
                case "fumbles:fumblesLost" -> fumblesLost += v;
                default -> { /* not used by the scoring formula */ }
            }
        }

        boolean isEmpty() {
            return passingYards == 0 && passingTouchdowns == 0 && interceptions == 0 && rushingYards == 0
                && rushingTouchdowns == 0 && receptions == 0 && receivingYards == 0 && receivingTouchdowns == 0
                && fumblesLost == 0;
        }

        PlayerWeeklyStatsDto toDto(String id, int season, int week) {
            return new PlayerWeeklyStatsDto(id, season, week, passingYards, passingTouchdowns, interceptions,
                rushingYards, rushingTouchdowns, receptions, receivingYards, receivingTouchdowns, fumblesLost);
        }
    }
}
