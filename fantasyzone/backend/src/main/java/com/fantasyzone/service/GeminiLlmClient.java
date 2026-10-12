package com.fantasyzone.service;

import com.fantasyzone.exception.LlmException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Calls Google's Gemini API (free tier, no billing). The key comes from the GEMINI_API_KEY environment variable,
 * stays on the server, and is never logged or sent to the browser. @Primary makes this the assistant's AI;
 * AnthropicLlmClient stays in the project but is no longer used.
 */
@Service
@Primary
public class GeminiLlmClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);

    private final RestClient client;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int maxOutputTokens;

    public GeminiLlmClient(@Qualifier("llmRestClient") RestClient client,
                           @Value("${gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
                           @Value("${gemini.api-key:}") String apiKey,
                           @Value("${gemini.model:gemini-3.8-flash}") String model,
                           @Value("${gemini.max-output-tokens:2048}") int maxOutputTokens) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
    }

    @Override
    public String modelName() { return model; }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmException(HttpStatus.SERVICE_UNAVAILABLE, "The AI assistant is not configured on this server.", null);
        }
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of("maxOutputTokens", maxOutputTokens));
        try {
            JsonNode response = client.post()
                    .uri(baseUrl + "/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
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
            for (JsonNode part : response.path("candidates").path(0).path("content").path("parts")) {
                if (!part.path("thought").asBoolean(false)) text.append(part.path("text").asText(""));
            }
        }
        String answer = text.toString().trim();
        if (answer.isEmpty()) {
            String reason = response == null ? "" : response.path("promptFeedback").path("blockReason")
                    .asText(response.path("candidates").path(0).path("finishReason").asText(""));
            log.warn("Gemini returned no text (reason: {})", reason);
            throw new LlmException(HttpStatus.BAD_GATEWAY, "The AI service did not return an answer. Try rephrasing the question.", null);
        }
        return answer;
    }

    private static LlmException fromProvider(RestClientResponseException e) {
        int code = e.getStatusCode().value();
        String detail = e.getResponseBodyAsString();
        log.warn("AI provider returned HTTP {}: {}", code, detail.length() > 300 ? detail.substring(0, 300) : detail);
        if (code == 429) {
            return new LlmException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The free AI limit was reached. Please try again in a minute (or tomorrow if the daily limit is used up).", e);
        }
        if (code == 401 || code == 403 || detail.contains("API_KEY_INVALID") || detail.contains("API key not valid")) {
            return new LlmException(HttpStatus.BAD_GATEWAY, "The AI service rejected this server's API key. Contact the administrator.", e);
        }
        return new LlmException(HttpStatus.BAD_GATEWAY, "The AI service could not answer right now.", e);
    }
}