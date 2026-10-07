package com.fantasyzone.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "matchups", indexes = @Index(name = "idx_matchup_league_week", columnList = "league_id,week"))
@Getter @Setter @NoArgsConstructor
public class Matchup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id")
    private League league;

    private int week;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private FantasyTeam homeTeam;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private FantasyTeam awayTeam;

    private Double homeScore;
    private Double awayScore;
    private boolean finalized = false;

    @PrePersist @PreUpdate
    void validateTeams() {
        if (homeTeam != null && homeTeam.equals(awayTeam)) {
            throw new IllegalStateException("A team cannot play itself");
        }
    }
}
