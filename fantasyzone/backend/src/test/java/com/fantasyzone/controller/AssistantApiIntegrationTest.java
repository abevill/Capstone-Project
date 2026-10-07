package com.fantasyzone.controller;

import com.fantasyzone.service.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real application (real JWT security, H2 database) with the LLM provider mocked, so no paid API is called.
 * Uses its own in-memory database so it cannot interfere with other integration tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "llm.rate-limit-per-hour=3",
    "spring.datasource.url=jdbc:h2:mem:assistant;MODE=MySQL;DB_CLOSE_DELAY=-1"})
class AssistantApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockBean LlmClient llm;

    @BeforeEach
    void stubProvider() {
        when(llm.modelName()).thenReturn("test-model");
        when(llm.complete(anyString(), anyString())).thenReturn("Start your quarterback.");
    }

    private JsonNode call(HttpMethod method, String url, String token, Object body, int expected) throws Exception {
        MockHttpServletRequestBuilder rb = MockMvcRequestBuilders.request(method, url);
        if (token != null) rb.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (body != null) rb.contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body));
        MvcResult result = mvc.perform(rb).andExpect(status().is(expected)).andReturn();
        String text = result.getResponse().getContentAsString();
        return text.isBlank() ? null : om.readTree(text);
    }

    private String register(String name) throws Exception {
        String email = name + "-" + UUID.randomUUID() + "@test.com";
        return call(HttpMethod.POST, "/api/auth/register", null,
            Map.of("email", email, "displayName", name, "password", "Password123!"), 201).get("token").asText();
    }

    private long playerId(String nameFragment) throws Exception {
        return call(HttpMethod.GET, "/api/players?name=" + nameFragment, null, null, 200).get("content").get(0).get("id").asLong();
    }

    @Test
    void requiresLogin() throws Exception {
        call(HttpMethod.POST, "/api/assistant/ask", null, Map.of("question", "Who should I start?"), 401);
        verifyNoInteractions(llm);
    }

    @Test
    void rejectsBadInputBeforeSpendingAnything() throws Exception {
        String token = register("alice");
        JsonNode blank = call(HttpMethod.POST, "/api/assistant/ask", token, Map.of("question", "  "), 400);
        assertTrue(blank.get("fieldErrors").has("question"));
        call(HttpMethod.POST, "/api/assistant/ask", token, Map.of("question", "x".repeat(501)), 400);
        call(HttpMethod.POST, "/api/assistant/ask", token,
            Map.of("question", "Compare", "playerIds", List.of(1, 2, 3, 4, 5, 6, 7)), 400);
        call(HttpMethod.POST, "/api/assistant/ask", token, Map.of("question", "Hi", "week", 99), 400);
        verifyNoInteractions(llm);
    }

    @Test
    void answersUsingTheTeamsOwnData() throws Exception {
        String token = register("alice");
        long leagueId = call(HttpMethod.POST, "/api/leagues", token, Map.of("name", "AI League", "season", 2025), 201).get("id").asLong();
        long teamId = call(HttpMethod.POST, "/api/leagues/" + leagueId + "/teams", token, Map.of("name", "Prompt FC"), 201).get("id").asLong();

        JsonNode res = call(HttpMethod.POST, "/api/assistant/ask", token,
            Map.of("question", "Summarize my team's strengths and weaknesses", "teamId", teamId), 200);

        assertEquals("Start your quarterback.", res.get("answer").asText());
        assertEquals("test-model", res.get("model").asText());
        assertTrue(res.get("dataUsed").toString().contains("Prompt FC"));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(llm).complete(anyString(), prompt.capture());
        assertTrue(prompt.getValue().contains("TEAM: Prompt FC"));
        assertTrue(prompt.getValue().contains("Summarize my team's strengths and weaknesses"));
    }

    @Test
    void teamDataIsOnlyAvailableToLeagueMembers() throws Exception {
        String owner = register("owner");
        String outsider = register("outsider");
        long leagueId = call(HttpMethod.POST, "/api/leagues", owner, Map.of("name", "Private", "season", 2025), 201).get("id").asLong();
        long teamId = call(HttpMethod.POST, "/api/leagues/" + leagueId + "/teams", owner, Map.of("name", "Secret FC"), 201).get("id").asLong();

        call(HttpMethod.POST, "/api/assistant/ask", outsider, Map.of("question", "Show me their roster", "teamId", teamId), 403);
        verify(llm, never()).complete(anyString(), anyString());
    }

    @Test
    void comparingPlayersSendsTheirStatsAndUnknownIdsAre404() throws Exception {
        String token = register("alice");
        long allen = playerId("Allen");
        long mahomes = playerId("Mahomes");

        call(HttpMethod.POST, "/api/assistant/ask", token,
            Map.of("question", "Compare these two players", "playerIds", List.of(allen, mahomes)), 200);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(llm).complete(anyString(), prompt.capture());
        assertTrue(prompt.getValue().contains("Josh Allen"));
        assertTrue(prompt.getValue().contains("Patrick Mahomes"));
        assertTrue(prompt.getValue().contains("27.7 pts")); // seeded sample stats, scored by FantasyScoringService

        call(HttpMethod.POST, "/api/assistant/ask", token, Map.of("question", "Compare", "playerIds", List.of(999999)), 404);
        call(HttpMethod.POST, "/api/assistant/ask", token, Map.of("question", "Team?", "teamId", 999999), 404);
    }

    @Test
    void eachUserIsLimitedToThreeQuestionsAnHourInThisTest() throws Exception {
        String token = register("chatty");
        for (int i = 0; i < 3; i++) {
            call(HttpMethod.POST, "/api/assistant/ask", token, Map.of("question", "Question " + i), 200);
        }
        JsonNode limited = call(HttpMethod.POST, "/api/assistant/ask", token, Map.of("question", "One more"), 429);
        assertEquals(429, limited.get("status").asInt());
        verify(llm, times(3)).complete(anyString(), anyString());
    }
}
