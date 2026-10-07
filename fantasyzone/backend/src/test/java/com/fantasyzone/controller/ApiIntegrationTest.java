package com.fantasyzone.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.fantasyzone.dto.NflPlayerDto;
import com.fantasyzone.dto.PlayerWeeklyStatsDto;
import com.fantasyzone.exception.SportsDataException;
import com.fantasyzone.service.SportsDataService;
import java.util.List;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end API tests against the real Spring context, real JWT security, and an in-memory H2 database. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @MockBean SportsDataService sports; // no real network calls in tests
    @Autowired ObjectMapper om;
    @Value("${app.admin-password}") String adminPassword;

    private record Account(String email, String token, long userId) {}

    private JsonNode call(HttpMethod method, String url, String token, Object body, int expected) throws Exception {
        MockHttpServletRequestBuilder rb = MockMvcRequestBuilders.request(method, url);
        if (token != null) rb.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (body != null) rb.contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body));
        MvcResult result = mvc.perform(rb).andExpect(status().is(expected)).andReturn();
        String text = result.getResponse().getContentAsString();
        return text.isBlank() ? null : om.readTree(text);
    }

    private Account register(String name) throws Exception {
        String email = name + "-" + UUID.randomUUID() + "@test.com";
        JsonNode r = call(HttpMethod.POST, "/api/auth/register", null,
            Map.of("email", email, "displayName", name, "password", "Password123!"), 201);
        return new Account(email, r.get("token").asText(), r.get("userId").asLong());
    }

    private long playerId(String nameFragment) throws Exception {
        return call(HttpMethod.GET, "/api/players?name=" + nameFragment, null, null, 200)
            .get("content").get(0).get("id").asLong();
    }

    // ---------- public vs protected ----------
    @Test
    void visitorsCanBrowsePublicDataButNotProtectedData() throws Exception {
        JsonNode players = call(HttpMethod.GET, "/api/players", null, null, 200);
        assertTrue(players.get("content").size() >= 12);
        call(HttpMethod.GET, "/api/players?position=QB", null, null, 200);
        call(HttpMethod.GET, "/api/leagues", null, null, 200);
        call(HttpMethod.GET, "/actuator/health", null, null, 200);

        call(HttpMethod.GET, "/api/me/leagues", null, null, 401);
        call(HttpMethod.POST, "/api/leagues", null, Map.of("name", "X", "season", 2025), 401);
        call(HttpMethod.GET, "/api/me/leagues", "garbage.token.value", null, 401);
    }

    // ---------- authentication ----------
    @Test
    void registerLoginAndValidationBehaveCorrectly() throws Exception {
        Account a = register("alice");
        JsonNode login = call(HttpMethod.POST, "/api/auth/login", null,
            Map.of("email", a.email(), "password", "Password123!"), 200);
        assertFalse(login.get("token").asText().isBlank());
        assertTrue(login.get("roles").toString().contains("ROLE_USER"));

        call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", a.email(), "password", "wrong-password"), 401);
        call(HttpMethod.POST, "/api/auth/register", null,
            Map.of("email", a.email(), "displayName", "alice", "password", "Password123!"), 409);

        JsonNode invalid = call(HttpMethod.POST, "/api/auth/register", null,
            Map.of("email", "not-an-email", "displayName", "x", "password", "short"), 400);
        assertTrue(invalid.has("fieldErrors"));
        assertTrue(invalid.get("fieldErrors").has("email"));
    }

    // ---------- the whole league workflow ----------
    @Test
    void fullLeagueWorkflowFromCreationToStandings() throws Exception {
        Account alice = register("alice");
        Account bob = register("bobby");

        // create + join
        JsonNode league = call(HttpMethod.POST, "/api/leagues", alice.token(), Map.of("name", "Test League", "season", 2025), 201);
        long leagueId = league.get("id").asLong();
        String joinCode = league.get("joinCode").asText();
        call(HttpMethod.POST, "/api/leagues/join", bob.token(), Map.of("joinCode", joinCode), 200);
        call(HttpMethod.POST, "/api/leagues/join", bob.token(), Map.of("joinCode", joinCode), 409);
        call(HttpMethod.POST, "/api/leagues/join", bob.token(), Map.of("joinCode", "BADCODE"), 404);
        assertEquals(2, call(HttpMethod.GET, "/api/leagues/" + leagueId + "/members", alice.token(), null, 200).size());
        assertNotNull(call(HttpMethod.GET, "/api/me/leagues", alice.token(), null, 200).get(0).get("joinCode"));

        // outsiders cannot see private league data
        Account carol = register("carol");
        call(HttpMethod.GET, "/api/leagues/" + leagueId + "/members", carol.token(), null, 403);
        call(HttpMethod.GET, "/api/leagues/" + leagueId + "/teams", carol.token(), null, 403);

        // teams
        long teamA = call(HttpMethod.POST, "/api/leagues/" + leagueId + "/teams", alice.token(), Map.of("name", "Alice FC"), 201).get("id").asLong();
        long teamB = call(HttpMethod.POST, "/api/leagues/" + leagueId + "/teams", bob.token(), Map.of("name", "Bob United"), 201).get("id").asLong();
        call(HttpMethod.POST, "/api/leagues/" + leagueId + "/teams", alice.token(), Map.of("name", "Second Team"), 409);
        call(HttpMethod.POST, "/api/leagues/" + leagueId + "/teams", carol.token(), Map.of("name", "Carol"), 403);

        // roster rules
        long allen = playerId("Allen");
        long mahomes = playerId("Mahomes");
        call(HttpMethod.POST, "/api/teams/" + teamA + "/roster/players/" + allen, alice.token(), Map.of("positionSlot", "QB"), 201);
        call(HttpMethod.POST, "/api/teams/" + teamB + "/roster/players/" + allen, bob.token(), Map.of("positionSlot", "QB"), 409);   // already taken
        call(HttpMethod.POST, "/api/teams/" + teamA + "/roster/players/" + mahomes, bob.token(), Map.of("positionSlot", "BENCH"), 403); // not his team
        assertEquals(1, call(HttpMethod.GET, "/api/teams/" + teamA + "/roster", bob.token(), null, 200).size());
        JsonNode history = call(HttpMethod.GET, "/api/teams/" + teamA + "/transactions", bob.token(), null, 200);
        assertEquals(1, history.size());
        assertEquals("ADD", history.get(0).get("type").asText());
        call(HttpMethod.GET, "/api/teams/" + teamA + "/transactions", carol.token(), null, 403);
        call(HttpMethod.POST, "/api/teams/" + teamA + "/roster/players/" + mahomes, alice.token(), Map.of("positionSlot", ""), 400);

        // roster lock
        call(HttpMethod.PUT, "/api/leagues/" + leagueId + "/lock?locked=true", bob.token(), null, 403);
        call(HttpMethod.PUT, "/api/leagues/" + leagueId + "/lock?locked=true", alice.token(), null, 200);
        call(HttpMethod.POST, "/api/teams/" + teamA + "/roster/players/" + mahomes, alice.token(), Map.of("positionSlot", "BENCH"), 409);
        call(HttpMethod.PUT, "/api/leagues/" + leagueId + "/lock?locked=false", alice.token(), null, 200);

        // matchups: only the commissioner can generate and score them
        call(HttpMethod.POST, "/api/leagues/" + leagueId + "/matchups/generate", bob.token(), null, 403);
        JsonNode generated = call(HttpMethod.POST, "/api/leagues/" + leagueId + "/matchups/generate", alice.token(), null, 201);
        assertEquals(1, generated.size());
        long matchupId = generated.get(0).get("id").asLong();
        call(HttpMethod.POST, "/api/leagues/" + leagueId + "/matchups/generate", alice.token(), null, 409);

        call(HttpMethod.PUT, "/api/matchups/" + matchupId + "/calculate", bob.token(), null, 403);
        JsonNode scored = call(HttpMethod.PUT, "/api/matchups/" + matchupId + "/calculate", alice.token(), null, 200);
        assertTrue(scored.get("finalized").asBoolean());
        call(HttpMethod.PUT, "/api/matchups/" + matchupId + "/calculate", alice.token(), null, 409);

        // standings are public: Alice's QB (27.7 points) beats Bob's empty roster
        JsonNode standings = call(HttpMethod.GET, "/api/leagues/" + leagueId + "/standings", null, null, 200);
        assertEquals("Alice FC", standings.get(0).get("teamName").asText());
        assertEquals(1, standings.get(0).get("wins").asInt());
        assertEquals(27.7, standings.get(0).get("pointsFor").asDouble(), 0.001);
        assertEquals(1, standings.get(1).get("losses").asInt());

        // a member whose team already has matchups cannot be removed
        call(HttpMethod.DELETE, "/api/leagues/" + leagueId + "/members/" + bob.userId(), alice.token(), null, 409);
        call(HttpMethod.DELETE, "/api/leagues/" + leagueId + "/members/" + bob.userId(), bob.token(), null, 403);

        // dropping players works for the owner
        call(HttpMethod.DELETE, "/api/teams/" + teamA + "/roster/players/" + allen, alice.token(), null, 204);
        call(HttpMethod.DELETE, "/api/teams/" + teamA + "/roster/players/" + allen, alice.token(), null, 404);
        JsonNode afterDrop = call(HttpMethod.GET, "/api/teams/" + teamA + "/transactions", alice.token(), null, 200);
        assertEquals(2, afterDrop.size());
        assertEquals("DROP", afterDrop.get(0).get("type").asText()); // newest first
    }

    @Test
    void unknownResourcesReturnConsistentNotFoundErrors() throws Exception {
        JsonNode err = call(HttpMethod.GET, "/api/leagues/999999", null, null, 404);
        assertEquals(404, err.get("status").asInt());
        assertTrue(err.has("message"));
        call(HttpMethod.GET, "/api/players/999999", null, null, 404);
        call(HttpMethod.GET, "/api/leagues/999999/standings", null, null, 404);
    }

    // ---------- admin ----------
    @Test
    void adminEndpointsAreRestrictedAndDisabledUsersAreLockedOut() throws Exception {
        Account user = register("regular");
        call(HttpMethod.GET, "/api/admin/users", user.token(), null, 403);
        call(HttpMethod.GET, "/api/admin/users", null, null, 401);

        JsonNode adminLogin = call(HttpMethod.POST, "/api/auth/login", null,
            Map.of("email", "admin@fantasyzone.test", "password", adminPassword), 200);
        assertTrue(adminLogin.get("roles").toString().contains("ROLE_ADMIN"));
        String admin = adminLogin.get("token").asText();

        assertTrue(call(HttpMethod.GET, "/api/admin/users", admin, null, 200).get("content").size() >= 2);

        JsonNode disabled = call(HttpMethod.PUT, "/api/admin/users/" + user.userId() + "/status?enabled=false", admin, null, 200);
        assertFalse(disabled.get("enabled").asBoolean());
        call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", user.email(), "password", "Password123!"), 401);
        call(HttpMethod.GET, "/api/me/leagues", user.token(), null, 401); // old token stops working too

        call(HttpMethod.PUT, "/api/admin/users/" + user.userId() + "/status?enabled=true", admin, null, 200);
        call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", user.email(), "password", "Password123!"), 200);

        long adminId = adminLogin.get("userId").asLong();
        call(HttpMethod.PUT, "/api/admin/users/" + adminId + "/status?enabled=false", admin, null, 409);
    }

    @Test
    void playerFiltersAndStatsWork() throws Exception {
        assertEquals(3, call(HttpMethod.GET, "/api/players?position=QB", null, null, 200).get("content").size());
        assertEquals(2, call(HttpMethod.GET, "/api/players?team=KC", null, null, 200).get("content").size());
        long id = playerId("Allen");
        JsonNode stats = call(HttpMethod.GET, "/api/players/" + id + "/stats", null, null, 200);
        assertEquals(27.7, stats.get(0).get("fantasyPoints").asDouble(), 0.001);
    }

    // ---------- real-data sync (provider mocked) ----------
    private String adminToken() throws Exception {
        return call(HttpMethod.POST, "/api/auth/login", null,
            Map.of("email", "admin@fantasyzone.test", "password", adminPassword), 200).get("token").asText();
    }

    @Test
    void adminCanSyncPlayersAndStatsFromTheProvider() throws Exception {
        String admin = adminToken();
        when(sports.getPlayers()).thenReturn(List.of(new NflPlayerDto("90001", "Sync Runner", "RB", "TST")));
        when(sports.getWeeklyStats(2026, 4)).thenReturn(List.of(
            new PlayerWeeklyStatsDto("90001", 2026, 4, 0, 0, 0, 100, 1, 3, 20, 0, 0),
            new PlayerWeeklyStatsDto("99999", 2026, 4, 250, 2, 0, 0, 0, 0, 0, 0, 0))); // second player is unknown

        JsonNode players = call(HttpMethod.POST, "/api/admin/players/sync", admin, null, 200);
        assertEquals(1, players.get("playersCreated").asInt());
        assertEquals(0, call(HttpMethod.POST, "/api/admin/players/sync", admin, null, 200).get("playersCreated").asInt()); // idempotent

        JsonNode stats = call(HttpMethod.POST, "/api/admin/stats/sync?season=2026&week=4", admin, null, 200);
        assertEquals(1, stats.get("statLinesCreated").asInt());
        assertEquals(1, stats.get("skippedUnknownPlayers").asInt());
        assertEquals(0, call(HttpMethod.POST, "/api/admin/stats/sync?season=2026&week=4", admin, null, 200).get("statLinesCreated").asInt());

        long id = call(HttpMethod.GET, "/api/players?name=Runner", null, null, 200).get("content").get(0).get("id").asLong();
        JsonNode line = call(HttpMethod.GET, "/api/players/" + id + "/stats", null, null, 200).get(0);
        assertEquals(2026, line.get("season").asInt());
        assertEquals(21.0, line.get("fantasyPoints").asDouble(), 0.001); // 10 + 6 rushing, 3 receptions, 2 receiving yards
    }

    @Test
    void syncRejectsBadInputRegularUsersAndProviderFailures() throws Exception {
        String admin = adminToken();
        call(HttpMethod.POST, "/api/admin/stats/sync?season=2026&week=99", admin, null, 400);

        Account user = register("regular");
        call(HttpMethod.POST, "/api/admin/players/sync", user.token(), null, 403);
        call(HttpMethod.POST, "/api/admin/stats/sync", null, null, 401);

        when(sports.getPlayers()).thenThrow(new SportsDataException("Could not get the roster of team 1 from ESPN", null));
        JsonNode err = call(HttpMethod.POST, "/api/admin/players/sync", admin, null, 502);
        assertEquals(502, err.get("status").asInt());
    }
}
