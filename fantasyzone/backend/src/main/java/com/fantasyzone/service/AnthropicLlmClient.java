package com.fantasyzone.service;

import com.fantasyzone.exception.LlmException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Calls the Anthropic Messages API. The API key comes from the LLM_API_KEY environment variable (property llm.api-key),
 * lives only in this server process, and is never logged or returned to the browser.
 */
@Service
public class AnthropicLlmClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmClient.class);
    private static final String API_VERSION = "2023-06-01";

    private final RestClient client;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public AnthropicLlmClient(@Qualifier("llmRestClient") RestClient client,
                              @Value("${llm.api-key:}") String apiKey,
                              @Value("${llm.model:claude-haiku-4-5-20251001}") String model,
                              @Value("${llm.max-tokens:700}") int maxTokens) {
        this.client = client;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @Override
    public String modelName() { return model; }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmException(HttpStatus.SERVICE_UNAVAILABLE, "The AI assistant is not configured on this server.", null);
        }
        Map<String, Object> body = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "system", systemPrompt,
            "messages", List.of(Map.of("role", "user", "content", userPrompt)));
        try {
            JsonNode response = client.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
            return extractText(response);
        } catch (RestClientResponseException e) {
            throw fromProvider(e);
        } catch (RestClientException e) {
            log.warn("AI service unreachable: {}", e.getMessage());
            throw new LlmException(HttpStatus.BAD_GATEWAY, "Could not reach the AI service. Please try again.", e);
        }
    }

    private static String extractText(JsonNode response) {
        StringBuilder text = new StringBuilder();
        if (response != null) {
            for (JsonNode block : response.path("content")) {
                if ("text".equals(block.path("type").asText())) text.append(block.path("text").asText(""));
            }
        }
        String answer = text.toString().trim();
        if (answer.isEmpty()) throw new LlmException(HttpStatus.BAD_GATEWAY, "The AI service returned an empty answer.", null);
        return answer;
    }

    private static LlmException fromProvider(RestClientResponseException e) {
        int code = e.getStatusCode().value();
        String detail = e.getResponseBodyAsString();
        log.warn("AI provider returned HTTP {}: {}", code, detail.length() > 300 ? detail.substring(0, 300) : detail);
        if (code == 429 || code == 529) {
            return new LlmException(HttpStatus.SERVICE_UNAVAILABLE, "The AI service is busy right now. Please try again in a minute.", e);
        }
        if (code == 401 || code == 403) {
            return new LlmException(HttpStatus.BAD_GATEWAY, "The AI service rejected this server's credentials. Contact the administrator.", e);
        }
        return new LlmException(HttpStatus.BAD_GATEWAY, "The AI service could not answer right now.", e);
    }
}
