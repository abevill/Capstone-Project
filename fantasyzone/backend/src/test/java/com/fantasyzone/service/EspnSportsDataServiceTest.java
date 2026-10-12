package com.fantasyzone.service;

import com.fantasyzone.dto.DTO;
import com.fantasyzone.dto.DTO.*;
import com.fantasyzone.exception.SportsDataException;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Parses real ESPN responses captured on 2026-10-06 (week 4 of the 2026 season), trimmed to the parts we read.
 * In scoreboard.json the second game (IND vs WSH) was edited to "not finished" to test that unfinished games are skipped.
 */
class EspnSportsDataServiceTest {
    private static final String BASE = "https://espn.test/nfl";
    private MockRestServiceServer server;
    private EspnSportsDataService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        service = new EspnSportsDataService(builder.build());
    }

    private String fixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/espn/" + name)) {
            assertNotNull(in, "missing fixture " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static PlayerWeeklyStatsDto line(List<PlayerWeeklyStatsDto> lines, String id) {
        return lines.stream().filter(l -> l.externalPlayerId().equals(id)).findFirst()
            .orElseThrow(() -> new AssertionError("no stat line for " + id));
    }

    // ---------- weekly stats ----------
    @Test
    void boxScoreIsConvertedToStatLines() throws Exception {
        server.expect(requestTo(containsString("/scoreboard?dates=2026&seasontype=2&week=4")))
            .andRespond(withSuccess(fixture("scoreboard.json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/summary?event=401872964")))   // only the finished game is requested
            .andRespond(withSuccess(fixture("summary.json"), MediaType.APPLICATION_JSON));

        List<PlayerWeeklyStatsDto> lines = service.getWeeklyStats(2026, 4);
        server.verify();

        PlayerWeeklyStatsDto rodgers = line(lines, "8439");
        assertEquals(299, rodgers.passingYards());
        assertEquals(3, rodgers.passingTouchdowns());
        assertEquals(2, rodgers.interceptions());
        assertEquals(2026, rodgers.season());
        assertEquals(4, rodgers.week());

        PlayerWeeklyStatsDto metcalf = line(lines, "4047650");
        assertEquals(5, metcalf.receptions());
        assertEquals(115, metcalf.receivingYards());
        assertEquals(0, metcalf.receivingTouchdowns());
    }

    @Test
    void oneAthleteInSeveralCategoriesIsMergedIntoOneLine() throws Exception {
        expectStatsRequests();
        List<PlayerWeeklyStatsDto> lines = service.getWeeklyStats(2026, 4);

        PlayerWeeklyStatsDto watson = line(lines, "3122840"); // passes, runs, and fumbles
        assertEquals(268, watson.passingYards());
        assertEquals(1, watson.passingTouchdowns());
        assertEquals(1, watson.interceptions());
        assertEquals(22, watson.rushingYards());
        assertEquals(1, watson.fumblesLost());

        PlayerWeeklyStatsDto judkins = line(lines, "4685702"); // rushes and catches
        assertEquals(53, judkins.rushingYards());
        assertEquals(1, judkins.rushingTouchdowns());
        assertEquals(6, judkins.receptions());
        assertEquals(43, judkins.receivingYards());
    }

    @Test
    void defensivePlayersAndAllZeroLinesAreIgnored() throws Exception {
        expectStatsRequests();
        List<PlayerWeeklyStatsDto> lines = service.getWeeklyStats(2026, 4);
        // T.J. Watt appears only under fumbles (0 lost) and defense: no offensive stat line
        assertTrue(lines.stream().noneMatch(l -> l.externalPlayerId().equals("3045282")));
        // the defensive "interceptions" category must not be counted as thrown interceptions
        assertTrue(lines.stream().noneMatch(l -> l.externalPlayerId().equals("4820584")));
    }

    @Test
    void weekWithNoFinishedGamesGivesNoLines() throws Exception {
        server.expect(requestTo(containsString("/scoreboard")))
            .andRespond(withSuccess("{\"events\":[{\"id\":\"1\",\"status\":{\"type\":{\"completed\":false}}}]}", MediaType.APPLICATION_JSON));
        assertTrue(service.getWeeklyStats(2026, 9).isEmpty());
        server.verify();
    }

    private void expectStatsRequests() throws Exception {
        server.expect(requestTo(containsString("/scoreboard"))).andRespond(withSuccess(fixture("scoreboard.json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/summary?event=401872964"))).andRespond(withSuccess(fixture("summary.json"), MediaType.APPLICATION_JSON));
    }

    // ---------- players ----------
    @Test
    void rostersYieldOnlyScorablePositionsWithoutDuplicates() throws Exception {
        server.expect(ExpectedCount.manyTimes(), requestTo(containsString("/roster")))
            .andRespond(withSuccess(fixture("roster.json"), MediaType.APPLICATION_JSON));

        List<NflPlayerDto> players = service.getPlayers();

        assertEquals(27, players.size()); // all 32 requests returned the same roster; duplicates collapse
        assertTrue(players.stream().allMatch(p -> Set.of("QB", "RB", "WR", "TE", "PK").contains(p.position())));
        assertTrue(players.stream().allMatch(p -> "GB".equals(p.nflTeam())));
        NflPlayerDto love = players.stream().filter(p -> p.externalId().equals("4036378")).findFirst().orElseThrow();
        assertEquals("Jordan Love", love.name());
        assertEquals("QB", love.position());
    }

    @Test
    void playerSyncSurvivesSomeFailedTeamRequests() throws Exception {
        server.expect(requestTo(containsString("/teams/1/roster"))).andRespond(withServerError());
        server.expect(ExpectedCount.manyTimes(), requestTo(containsString("/roster")))
            .andRespond(withSuccess(fixture("roster.json"), MediaType.APPLICATION_JSON));
        assertFalse(service.getPlayers().isEmpty());
    }

    @Test
    void playerSyncFailsWhenNothingCameBack() {
        server.expect(ExpectedCount.manyTimes(), requestTo(containsString("/roster"))).andRespond(withServerError());
        assertThrows(SportsDataException.class, () -> service.getPlayers());
    }

    // ---------- current week and failures ----------
    @Test
    void currentWeekComesFromTheScoreboard() throws Exception {
        server.expect(requestTo(containsString("/scoreboard"))).andRespond(withSuccess(fixture("scoreboard.json"), MediaType.APPLICATION_JSON));
        assertEquals(new SeasonWeek(2026, 4), service.currentWeek());
    }

    @Test
    void currentWeekFailsClearlyWhenResponseIsUnusable() {
        server.expect(requestTo(containsString("/scoreboard"))).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThrows(SportsDataException.class, () -> service.currentWeek());
    }

    @Test
    void providerErrorsBecomeSportsDataExceptions() {
        server.expect(requestTo(containsString("/scoreboard"))).andRespond(withServerError());
        SportsDataException e = assertThrows(SportsDataException.class, () -> service.getWeeklyStats(2026, 4));
        assertTrue(e.getMessage().contains("scoreboard"));
    }

    @Test
    void failureOnOneBoxScoreFailsTheWholeWeek() throws Exception {
        server.expect(requestTo(containsString("/scoreboard"))).andRespond(withSuccess(fixture("scoreboard.json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/summary"))).andRespond(withStatus(org.springframework.http.HttpStatus.FORBIDDEN));
        assertThrows(SportsDataException.class, () -> service.getWeeklyStats(2026, 4));
    }
}
