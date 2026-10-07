package com.fantasyzone.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "fantasy_teams", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"league_id", "name"}),
    @UniqueConstraint(columnNames = {"league_id", "owner_id"})})
@Getter @Setter @NoArgsConstructor
public class FantasyTeam {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 60)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id")
    private League league;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    private int wins;
    private int losses;
    private int ties;
    private double pointsFor;

    // One fantasy team has many roster spots
    @OneToMany(mappedBy = "fantasyTeam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RosterPlayer> roster = new ArrayList<>();

    // Roster history is removed together with the team
    @OneToMany(mappedBy = "fantasyTeam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RosterTransaction> transactions = new ArrayList<>();
}
