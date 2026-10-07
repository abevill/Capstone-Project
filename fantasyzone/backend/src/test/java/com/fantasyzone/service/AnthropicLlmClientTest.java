package com.fantasyzone.service;

import com.fantasyzone.exception.LlmException;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AnthropicLlmClientTest {
    private MockRestServiceServer server;

    private AnthropicLlmClient client(String key) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://llm.test");
        server = MockRestServiceServer.bindTo(builder).build();
        return new AnthropicLlmClient(builder.build(), key, "test-model", 300);
    }

    @Test
    void sendsKeyModelAndPromptsThenReturnsTheText() {
        AnthropicLlmClient c = client("secret-key");
        server.expect(requestTo("https://llm.test/v1/messages"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("x-api-key", "secret-key"))
            .andExpect(header("anthropic-version", "2023-06-01"))
            .andExpect(jsonPath("$.model").value("test-model"))
            .andExpect(jsonPath("$.max_tokens").value(300))
            .andExpect(jsonPath("$.system").value("the rules"))
            .andExpect(jsonPath("$.messages[0].role").value("user"))
            .andExpect(jsonPath("$.messages[0].content").value("the question"))
            .andRespond(withSuccess("{\"content\":[{\"type\":\"text\",\"text\":\"Start Allen.\"}]}", MediaType.APPLICATION_JSON));

        assertEquals("Start Allen.", c.complete("the rules", "the question"));
        assertEquals("test-model", c.modelName());
        server.verify();
    }

    @Test
    void joinsTextBlocksAndIgnoresOtherBlockTypes() {
        AnthropicLlmClient c = client("k");
        server.expect(requestTo("https://llm.test/v1/messages")).andRespond(withSuccess(
            "{\"content\":[{\"type\":\"text\",\"text\":\"Part one. \"},{\"type\":\"tool_use\",\"id\":\"x\"},{\"type\":\"text\",\"text\":\"Part two.\"}]}",
            MediaType.APPLICATION_JSON));
        assertEquals("Part one. Part two.", c.complete("s", "q"));
    }

    @Test
    void missingKeyFailsWithoutCallingTheProvider() {
        AnthropicLlmClient c = client("");
        LlmException e = assertThrows(LlmException.class, () -> c.complete("s", "q"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
        server.verify(); // no request was expected, and none was made
    }

    @Test
    void rejectedCredentialsAreReportedWithoutLeakingDetails() {
        AnthropicLlmClient c = client("secret-key");
        server.expect(requestTo("https://llm.test/v1/messages")).andRespond(
            withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"message\":\"invalid x-api-key secret-key\"}}"));
        LlmException e = assertThrows(LlmException.class, () -> c.complete("s", "q"));
        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
        assertFalse(e.getMessage().contains("secret-key"));
    }

    @Test
    void providerRateLimitBecomesServiceUnavailable() {
        AnthropicLlmClient c = client("k");
        server.expect(requestTo("https://llm.test/v1/messages")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(LlmException.class, () -> c.complete("s", "q")).getStatus());
    }

    @Test
    void providerServerErrorBecomesBadGateway() {
        AnthropicLlmClient c = client("k");
        server.expect(requestTo("https://llm.test/v1/messages")).andRespond(withServerError());
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(LlmException.class, () -> c.complete("s", "q")).getStatus());
    }

    @Test
    void networkFailureBecomesBadGateway() {
        AnthropicLlmClient c = client("k");
        server.expect(requestTo("https://llm.test/v1/messages")).andRespond(withException(new IOException("boom")));
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(LlmException.class, () -> c.complete("s", "q")).getStatus());
    }

    @Test
    void emptyAnswerIsAnError() {
        AnthropicLlmClient c = client("k");
        server.expect(requestTo("https://llm.test/v1/messages")).andRespond(withSuccess("{\"content\":[]}", MediaType.APPLICATION_JSON));
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(LlmException.class, () -> c.complete("s", "q")).getStatus());
    }
}
