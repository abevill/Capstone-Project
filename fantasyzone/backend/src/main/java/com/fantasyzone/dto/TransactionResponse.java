package com.fantasyzone.dto;

import java.time.Instant;

public record TransactionResponse(Long id, String type, Long playerId, String playerName, String position,
                                  String slot, String madeBy, Instant at) {}
