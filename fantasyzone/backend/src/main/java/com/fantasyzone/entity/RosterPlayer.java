package com.fantasyzone.entity;

import jakarta.persistence.*;
import lombok.*;

/** A roster spot: many RosterPlayers belong to one FantasyTeam (many-to-one). */
@Entity
@Table(name = "roster_players",
       uniqueConstraints = @UniqueConstraint(columnNames = {"fantasy_team_id", "nfl_player_id"}))
@Getter @Setter @NoArgsConstructor
public class RosterPlayer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fantasy_team_id")
    private FantasyTeam fantasyTeam;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "nfl_player_id")
    private NflPlayer nflPlayer;

    @Column(nullable = false, length = 10)
    private String positionSlot; // QB, RB1, RB2, WR1, TE, FLEX, BENCH
}
