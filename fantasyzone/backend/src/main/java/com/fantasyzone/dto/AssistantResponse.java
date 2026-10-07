package com.fantasyzone.dto;

import java.util.List;

/** answer: the assistant's text. model: which model produced it. dataUsed: what FantasyZone data was sent along with the question. */
public record AssistantResponse(String answer, String model, List<String> dataUsed) {}
