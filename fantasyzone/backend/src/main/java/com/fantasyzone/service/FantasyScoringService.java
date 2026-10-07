package com.fantasyzone.service;

import com.fantasyzone.entity.PlayerWeeklyStats;
import org.springframework.stereotype.Service;

/** Business logic: the API gives raw stats, WE decide what they are worth. */
@Service
public class FantasyScoringService {

    public double calculateFantasyPoints(PlayerWeeklyStats s) {
        double points =
              s.getPassingYards() * 0.04
            + s.getPassingTouchdowns() * 4
            - s.getInterceptions() * 2
            + s.getRushingYards() * 0.10
            + s.getRushingTouchdowns() * 6
            + s.getReceptions()
            + s.getReceivingYards() * 0.10
            + s.getReceivingTouchdowns() * 6
            - s.getFumblesLost() * 2;
        return Math.round(points * 100.0) / 100.0;
    }
}
