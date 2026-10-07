package com.fantasyzone.entity;

import jakarta.persistence.*;
import lombok.*;

/** Raw real-world stats from the API. Fantasy points are NOT stored here; the service layer calculates them. */
@Entity
@Table(name = "player_weekly_stats",
       uniqueConstraints = @UniqueConstraint(columnNames = {"nfl_player_id", "season", "week"}))
@Getter @Setter @NoArgsConstructor
public class PlayerWeeklyStats {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "nfl_player_id")
    private NflPlayer nflPlayer;

    private int season;
    private int week;

    private int passingYards;
    private int passingTouchdowns;
    private int interceptions;
    private int rushingYards;
    private int rushingTouchdowns;
    private int receptions;
    private int receivingYards;
    private int receivingTouchdowns;
    private int fumblesLost;
}
