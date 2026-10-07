package com.fantasyzone.service;

/** Boundary to whichever LLM provider is used. Swap providers by writing another implementation of this interface. */
public interface LlmClient {
    /** Sends one question with instructions and returns the model's text answer. */
    String complete(String systemPrompt, String userPrompt);

    /** The model name, for display and debugging. */
    String modelName();
}
