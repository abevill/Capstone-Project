package com.fantasyzone.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** HTTP client for the LLM provider. The API key is NOT set here; it is added per request by the client class. */
@Configuration
public class LlmConfig {

    @Bean("llmRestClient")
    RestClient llmRestClient(@Value("${llm.base-url:https://api.anthropic.com}") String baseUrl,
                             @Value("${llm.timeout-ms:60000}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .defaultHeader(HttpHeaders.USER_AGENT, "FantasyZone-capstone/1.0 (student project)")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}
