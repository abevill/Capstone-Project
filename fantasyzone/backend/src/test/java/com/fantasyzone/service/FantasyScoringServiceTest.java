package com.fantasyzone.service;

import com.fantasyzone.entity.PlayerWeeklyStats;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FantasyScoringServiceTest {
    private final FantasyScoringService service = new FantasyScoringService();

    @Test
    void quarterbackGameIsScoredCorrectly() {
        PlayerWeeklyStats s = new PlayerWeeklyStats();
        s.setPassingYards(325);
        s.setPassingTouchdowns(3);
        s.setInterceptions(1);
        // 325*0.04 + 3*4 - 1*2 = 13 + 12 - 2 = 23
        assertEquals(23.0, service.calculateFantasyPoints(s), 0.001);
    }

    @Test
    void emptyStatLineIsZero() {
        assertEquals(0.0, service.calculateFantasyPoints(new PlayerWeeklyStats()), 0.001);
    }

    @Test
    void fumblesAndReceivingCount() {
        PlayerWeeklyStats s = new PlayerWeeklyStats();
        s.setReceptions(8); s.setReceivingYards(100); s.setReceivingTouchdowns(1); s.setFumblesLost(1);
        // 8 + 10 + 6 - 2 = 22
        assertEquals(22.0, service.calculateFantasyPoints(s), 0.001);
    }
}
