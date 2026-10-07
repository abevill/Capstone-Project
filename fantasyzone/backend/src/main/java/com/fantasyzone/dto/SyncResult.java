package com.fantasyzone.dto;

public record SyncResult(int playersCreated, int playersUpdated, int statLinesCreated, int statLinesUpdated,
                         int skippedUnknownPlayers) {
    public static SyncResult players(int created, int updated) { return new SyncResult(created, updated, 0, 0, 0); }
    public static SyncResult stats(int created, int updated, int skipped) { return new SyncResult(0, 0, created, updated, skipped); }
}
