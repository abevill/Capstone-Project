package com.fantasyzone.service;

import com.fantasyzone.entity.PlayerWeeklyStats;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FantasyScoringServiceExtraTest {
    private final FantasyScoringService service = new FantasyScoringService();

    @Test
    void rushingGameIsScoredCorrectly() {
        PlayerWeeklyStats s = new PlayerWeeklyStats();
        s.setRushingYards(110);
        s.setRushingTouchdowns(1);
        // 11 + 6
        assertEquals(17.0, service.calculateFantasyPoints(s), 0.001);
    }

    @Test
    void negativeGamesAreAllowed() {
        PlayerWeeklyStats s = new PlayerWeeklyStats();
        s.setInterceptions(3);
        s.setFumblesLost(2);
        assertEquals(-10.0, service.calculateFantasyPoints(s), 0.001);
    }

    @Test
    void resultIsRoundedToTwoDecimals() {
        PlayerWeeklyStats s = new PlayerWeeklyStats();
        s.setPassingYards(333); // 13.32
        assertEquals(13.32, service.calculateFantasyPoints(s), 0.0001);
    }
}
