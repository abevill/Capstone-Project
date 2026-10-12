package com.fantasyzone.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The lineup rules in one place: 8 starting spots (1 QB, 2 RB, 2 WR, 1 TE, 1 K, 1 FLEX that can be an RB or WR)
 * plus a bench, and at most 15 players on a roster. When a player is added, the server picks their spot.
 */
public final class LineupRules {
    public static final int MAX_ROSTER_SIZE = 15;
    public static final String FLEX = "FLEX";
    public static final String BENCH = "BENCH";

    /** Starting spots for each position. */
    private static final Map<String, Integer> STARTERS = Map.of("QB", 1, "RB", 2, "WR", 2, "TE", 1, "K", 1);
    private static final Set<String> FLEX_POSITIONS = Set.of("RB", "WR");

    private LineupRules() {}

    /**
     * Where a newly added player goes: their own starting spot if one is open, otherwise FLEX (RB and WR only),
     * otherwise the bench. currentSlots are the slots already used on the team (null entries are ignored).
     * Slot names: QB, RB1, RB2, WR1, WR2, TE, K, FLEX, BENCH. Older single names like "RB" still count as one RB spot.
     */
    public static String assignSlot(String position, Collection<String> currentSlots) {
        String pos = normalize(position);
        Integer spots = STARTERS.get(pos);
        if (spots == null) return BENCH; // positions with no starting spot, such as team defenses

        Set<String> taken = new HashSet<>();
        for (String slot : currentSlots) {
            if (slot != null) taken.add(slot.trim().toUpperCase(Locale.ROOT));
        }
        long filled = taken.stream().filter(s -> baseOf(s).equals(pos)).count();
        if (filled < spots) return freeSpotName(pos, spots, taken);
        if (FLEX_POSITIONS.contains(pos) && !taken.contains(FLEX)) return FLEX;
        return BENCH;
    }

    /** True for every slot except the bench. Bench players do not score. */
    public static boolean isStarterSlot(String slot) {
        return slot != null && !BENCH.equalsIgnoreCase(slot.trim());
    }

    private static String freeSpotName(String pos, int spots, Set<String> taken) {
        if (spots == 1) return pos;
        for (int i = 1; i <= spots; i++) {
            String name = pos + i;
            if (!taken.contains(name)) return name;
        }
        return pos;
    }

    private static String normalize(String position) {
        String pos = position == null ? "" : position.trim().toUpperCase(Locale.ROOT);
        return pos.equals("PK") ? "K" : pos; // ESPN calls kickers "PK"
    }

    private static String baseOf(String slot) {
        return slot.replaceAll("\\d+$", "");
    }
}