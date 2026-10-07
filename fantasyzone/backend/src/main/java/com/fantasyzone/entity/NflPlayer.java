package com.fantasyzone.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nfl_players", indexes = {
    @Index(name = "idx_player_position", columnList = "position"),
    @Index(name = "idx_player_team", columnList = "nflTeam")})
@Getter @Setter @NoArgsConstructor
public class NflPlayer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String externalId; // id from the sports data API

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 3)
    private String position; // QB, RB, WR, TE, K, DEF

    @Column(length = 4)
    private String nflTeam;
}
